package ar.edu.itba.pdc.chinese_whispers.application;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * This class implements a method to get different ids each time it is called.
 */
public class IdGenerator {

//TODO delete
//    /**
//     * Holds the singleton instance.
//     */
//    private static IdGenerator singleton;
//
//    /**
//     * Private constructor to implement singleton pattern.
//     */
//    private IdGenerator() {}
//
//
//    public static IdGenerator getInstance() {
//        if (singleton == null) {
//            singleton = new IdGenerator();
//        }
//        return singleton;
//    }



    /**
     * This set contains all ids already used
     */
    private final static Set<String> usedIds = new HashSet<>();

    /**
     * Random to get random values
     */
    private final static Random random = new Random();

    /**
     * To get different ids each time, when generating a random value,
     * it is checked if the {@code usedIds} set contains that value.
     * If the set contains the generated value, another value is generated.
     * It might happen (but it is not probable) to enter an infinite loop,
     * so the process is repeated, at most, the amount of times this constant states.
     */
    private final static int MAX_RANDOM_TRIES = 100;
    /**
     * In case the count reached the {@link IdGenerator#MAX_RANDOM_TRIES} value,
     * a fallback strategy is done.
     */
    private static int fallbackIds = 0;


    /**
     * Generates a random and unique id.
     *
     * @return A Stream Id (RFC 6120, section 4.7.3) for the response stream.
     */
    public static String generateId() {

        String result;
        int count = 0;
        do {
            long aux = random.nextLong();
            if (aux < 0) {
                aux *= -1; // Non negative ids.
            }
            result = String.valueOf(aux);
            count++;
        }
        while (usedIds.contains(result) && count < MAX_RANDOM_TRIES);
        if (count >= MAX_RANDOM_TRIES) {
            result = "NotRandom" + String.valueOf(fallbackIds++);
        }
        usedIds.add(result);
        return result;
    }

}
