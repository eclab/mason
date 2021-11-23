/*
  Copyright 2021 by George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.app.destest;

import sim.engine.*;
import java.util.*;
import sim.des.*;



class Main
{
    public static void main(String[] args)
    {
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
