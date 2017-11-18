package Util;

import java.util.*;

/**
 * Created by xrusa on 24/5/2017.
 */
public class BooleanOperations {
    public static Set<Integer> piecesUserHas (boolean[] array){
        Set<Integer> indexes= new HashSet<>();
        for(int i=0;i<array.length;i++){
            if(array[i]){
                indexes.add(i);
            }
        }
        return  indexes;
    }

    public  static  Set<Integer> piecesUserIsMissing(boolean[] array){
        Set<Integer> indexes= new HashSet<>();
        for(int i=0;i<array.length;i++){
            if(!array[i]){
                indexes.add(i);
            }
        }
        return  indexes;
    }

    public  static List<Integer> compareArrayRightHasWhatLeftMisses (boolean [] array1, boolean[] array2){
        Set<Integer> indexesLeftMisses= new HashSet<>();
        Set<Integer> indexesRightHas= new HashSet<>();

        indexesLeftMisses= piecesUserIsMissing(array1);
        indexesRightHas=piecesUserHas(array2);

        indexesLeftMisses.retainAll(indexesRightHas); //keeps only common
        return new ArrayList<>(indexesLeftMisses);
    }


    public void setBitAtIndex(BitSet set, int index){
        set.set(index);
    }



    public static boolean[] createBooleanOfCompletedFile(int pieces) {
        boolean[] array= new boolean[pieces];
        Arrays.fill(array,true);
        return array;
    }

}
