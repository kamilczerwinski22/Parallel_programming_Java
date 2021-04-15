import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Optimizer implements OptimizerInterface, Runnable {

    private static AtomicInteger dead_threads_counter;
    private static AtomicInteger sleeping_threads_counter;
    private static AtomicInteger all_threads_counter;
    private static AtomicInteger max_threads_num;
    private static AtomicBoolean suspended;
    private static final Object suspend_obj = new Object();

    private PawnInterface pawn;
    static volatile BoardInterface board;
    private int pos_col;
    private int pos_row;
    private int meeting_point_row;
    private int meeting_point_col;
    private static volatile ObjTakenKilled[][] lock_arr;
    private boolean dead;
    private boolean asleep;

    Optimizer(){
    }

    private Optimizer(PawnInterface pawn, BoardInterface board, int col, int row, ObjTakenKilled[][] lock_arr, int meeting_point_row
            , int meeting_point_col) {
        this.pawn = pawn;
        this.board = board;
        this.pos_col = col;
        this.pos_row = row;
        this.lock_arr = lock_arr;
        this.meeting_point_row = meeting_point_row;
        this.meeting_point_col = meeting_point_col;
        this.dead = false;
        this.asleep = false;
    }


    @Override
    public void setBoard(BoardInterface board) {

        ArrayList<Thread> arr_of_threads = new ArrayList<Thread>();

        dead_threads_counter = new AtomicInteger(0);
        all_threads_counter = new AtomicInteger(0);
        sleeping_threads_counter = new AtomicInteger(0);
        max_threads_num = new AtomicInteger(0);
        suspended = new AtomicBoolean(false);

        // create arr of lock object
        ObjTakenKilled[][] lock_arr = new ObjTakenKilled[board.getSize()][board.getSize()];
        for (int i = 0; i < lock_arr.length; i++){
            for (int j = 0; j< lock_arr.length; j++){
                lock_arr[i][j] = new ObjTakenKilled();
            }
        }

        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                if (board.get(col, row).isPresent()) {
                    lock_arr[row][col].setTaken(1);
                    all_threads_counter.getAndIncrement();
                    max_threads_num.getAndIncrement();
                    Thread thread = new Thread(new Optimizer(board.get(col, row).get(), board, col, row,
                            lock_arr, board.getMeetingPointRow(), board.getMeetingPointCol()));
                    board.get(col, row).get().registerThread(thread);
                    arr_of_threads.add(thread);
                }
            }
        }

        // start optimalization and all threads
        board.optimizationStart();
//        System.out.println("Optymalizacja wystartowała");
        for (Thread thread: arr_of_threads){
            thread.start();
        }

        while (true){
            if (dead_threads_counter.get() == max_threads_num.get()){
//                System.out.println("Koniec opymalizacji");
                board.optimizationDone();
                break;
            }
        }

    }

    boolean make_move() {
        synchronized (lock_arr[pos_row][pos_col]){
            if (meeting_point_col > pos_col){  // w prawo
                synchronized (lock_arr[pos_row][pos_col + 1]){
                    if(lock_arr[pos_row][pos_col + 1].getTaken() == 0){
                        pawn.moveRight();
                        lock_arr[pos_row][pos_col + 1].setTaken(1);
                        lock_arr[pos_row][pos_col].setTaken(0);
                        pos_col += 1;
                        return true;
                    }
                }
            }
            if (meeting_point_col < pos_col){  // w lewo
                synchronized (lock_arr[pos_row][pos_col - 1]){
                    if(lock_arr[pos_row][pos_col - 1].getTaken() == 0){
                        pawn.moveLeft();
                        lock_arr[pos_row][pos_col - 1].setTaken(1);
                        lock_arr[pos_row][pos_col].setTaken(0);
                        pos_col -= 1;
                        return true;
                    }
                }
            }
            if (meeting_point_row > pos_row){  // w górę (trzeba dać move down)
                synchronized (lock_arr[pos_row + 1][pos_col]){
                    if(lock_arr[pos_row + 1][pos_col].getTaken() == 0){
                        pawn.moveUp();
                        lock_arr[pos_row + 1][pos_col].setTaken(1);
                        lock_arr[pos_row][pos_col].setTaken(0);
                        pos_row += 1;
                        return true;
                    }
                }
            }
            if (meeting_point_row < pos_row){  // w dół
                synchronized (lock_arr[pos_row - 1][pos_col]){
                    if(lock_arr[pos_row - 1][pos_col].getTaken() == 0){
                        pawn.moveDown();
                        lock_arr[pos_row - 1][pos_col].setTaken(1);
                        lock_arr[pos_row][pos_col].setTaken(0);
                        pos_row -= 1;
                        return true;
                    }
                }
            }

        }
        return false; // no movement
    }

    boolean check_if_killed(){
        synchronized (lock_arr[pos_row][pos_col]){
            // pawn in the meeting point
            if (pos_row == meeting_point_row && pos_col == meeting_point_col){
                lock_arr[pos_row][pos_col].setKilled();
                return true;
            }

            // we are on the good row, but check if we can to through cols
            if (pos_row == meeting_point_row){
                if (pos_col < meeting_point_col){
                    synchronized (lock_arr[pos_row][pos_col + 1]){
                        if (lock_arr[pos_row][pos_col + 1].getKilled() == 1){
                            lock_arr[pos_row][pos_col].setKilled();
                            return true;
                        }
                    }
                }
                if (pos_col > meeting_point_col){
                    synchronized (lock_arr[pos_row][pos_col - 1]){
                        if (lock_arr[pos_row][pos_col - 1].getKilled() == 1){
                            lock_arr[pos_row][pos_col].setKilled();
                            return true;
                        }
                    }
                }
            }

            // we are on the good col, but check if we can to through rows
            if (pos_col == meeting_point_col) {
                if (pos_row < meeting_point_row) {
                    synchronized (lock_arr[pos_row + 1][pos_col]) {
                        if (lock_arr[pos_row + 1][pos_col].getKilled() == 1) {
                            lock_arr[pos_row][pos_col].setKilled();
                            return true;
                        }
                    }
                }
                if (pos_row > meeting_point_row) {
                    synchronized (lock_arr[pos_row - 1][pos_col]) {
                        if (lock_arr[pos_row - 1][pos_col].getKilled() == 1) {
                            lock_arr[pos_row][pos_col].setKilled();
                            return true;
                        }
                    }
                }
            }

                // check diagonals at each quarter
                if (meeting_point_row > pos_row && meeting_point_col > pos_col){  // from left + bottom
                    synchronized (lock_arr[pos_row + 1][pos_col]){
                        synchronized (lock_arr[pos_row][pos_col + 1]){
                            if (lock_arr[pos_row + 1][pos_col].getKilled() == 1 &&
                                    lock_arr[pos_row][pos_col + 1].getKilled() == 1){
                                lock_arr[pos_row][pos_col].setKilled();
                                return true;
                            }
                        }
                    }
                }

                if (meeting_point_row > pos_row && meeting_point_col < pos_col){  // from right + bottom
                    synchronized (lock_arr[pos_row + 1][pos_col]){
                        synchronized (lock_arr[pos_row][pos_col - 1]){
                            if (lock_arr[pos_row + 1][pos_col].getKilled() == 1 &&
                                    lock_arr[pos_row][pos_col - 1].getKilled() == 1){
                                lock_arr[pos_row][pos_col].setKilled();
                                return true;
                            }
                        }
                    }
                }

                if (meeting_point_row < pos_row && meeting_point_col > pos_col){  // from left + top
                    synchronized (lock_arr[pos_row - 1][pos_col]){
                        synchronized (lock_arr[pos_row][pos_col + 1]){
                            if (lock_arr[pos_row - 1][pos_col].getKilled() == 1 &&
                                    lock_arr[pos_row][pos_col + 1].getKilled() == 1){
                                lock_arr[pos_row][pos_col].setKilled();
                                return true;
                            }
                        }
                    }
                }

                if (meeting_point_row < pos_row && meeting_point_col < pos_col){  // from right + top
                    synchronized (lock_arr[pos_row - 1][pos_col]){
                        synchronized (lock_arr[pos_row][pos_col - 1]){
                            if (lock_arr[pos_row - 1][pos_col].getKilled() == 1 &&
                                    lock_arr[pos_row][pos_col - 1].getKilled() == 1){
                                lock_arr[pos_row][pos_col].setKilled();
                                return true;
                            }
                        }
                    }
                }
            return false;  // if it's not killed
            }

        }

    @Override
    public void run() {
        while (!dead) {
            if (suspended.get()){
                synchronized (suspend_obj){
                    sleeping_threads_counter.incrementAndGet();
                    try {
                        suspend_obj.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                asleep = false;
                continue;
            }

            synchronized (lock_arr[pos_row][pos_col]) {  // current obj synchro
                boolean move;
                move = make_move();
                if(!move){ // move has been done
                    if (check_if_killed()){
                        dead = true;
                        dead_threads_counter.incrementAndGet();
                        all_threads_counter.decrementAndGet();
                    } else {
                        asleep = true;
                    }
                }

                synchronized (suspend_obj){
                    suspend_obj.notifyAll();
                    sleeping_threads_counter.set(0);
                }
            }

            if (asleep){
                synchronized (suspend_obj){
                    sleeping_threads_counter.incrementAndGet();
                    try {
                        suspend_obj.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                asleep = false;
            }
        }
    }


    @Override
    public void suspend() {
        synchronized (suspend_obj){
            suspended.set(true);
            suspend_obj.notifyAll();
        }
        while (true){
            if (all_threads_counter.get() == sleeping_threads_counter.get()){
                break;
            }
        }
    }

    @Override
    public void resume() {
        synchronized (suspend_obj){
            suspended.set(false);
            suspend_obj.notifyAll();
        }
    }

}

class ObjTakenKilled {

    volatile private int taken;
    volatile private int killed;

    ObjTakenKilled() {
        taken = 0;
        killed = 0;
    }

    void setKilled() {
        this.killed = 1;
    }

    int getKilled() {
        return killed;
    }

    void setTaken(int taken) {
        this.taken = taken;
    }

    int getTaken() {
        return taken;
    }
}