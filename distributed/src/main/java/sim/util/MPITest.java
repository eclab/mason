package sim.util;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import mpi.*;

public class MPITest {

    private static final Comm comm = MPI.COMM_WORLD;

    private MPITest() {}

    public static void execInOrder(Consumer<Integer> func, int delay) {
        try {
            for (int i = 0; i < comm.getSize(); i++) {
                execOnlyIn(i, func);
                TimeUnit.MILLISECONDS.sleep(delay);
            }
        } catch (MPIException | InterruptedException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void execOnlyIn(int pid, Consumer<Integer> func) {
        try {
            if (pid == comm.getRank())
                func.accept(pid);
            comm.barrier();
        } catch (MPIException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void printInOrder(String s) {
        execInOrder(i -> System.out.printf("[%2d] %s\n", i, s), 0);
    }

    public static void printOnlyIn(int pid, String s) {
        execOnlyIn(pid, i -> System.out.printf("[%2d] %s\n", i, s));
    }
}
