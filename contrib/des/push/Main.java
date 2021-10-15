import sim.engine.*;
import java.util.*;



class Main{

    public static void main(String[] args){
        // wood to chair transformer example
        CountableResource wood = new CountableResource("wood", 100);
        CountableResource chairs = new CountableResource("chair", 0);

        Transformer woodToChair = new Transformer(null, chairs.duplicate0(), wood.duplicate0(), 5, 1);

        System.out.println(woodToChair.getName());


        Sink chairStorage = new Sink(null, chairs.duplicate0());

        chairStorage.accept(woodToChair, chairs.duplicate0(), 20, 20);

        //wood should be 0

        System.out.println(wood.toString());

    }
}