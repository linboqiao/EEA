package edu.cmu.cs.lti.cds.utils;

import edu.cmu.cs.lti.cds.model.MooneyEventRepre;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TObjectIntMap;
import org.apache.commons.lang3.tuple.Pair;
import org.mapdb.Fun;
import weka.core.SerializationHelper;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/31/14
 * Time: 11:16 PM
 */
public class MultiMapUtils {
    public static int getTf(Map<String, Fun.Tuple2<Integer, Integer>>[] maps, String key) {
        int tfSum = 0;
        for (Map<String, Fun.Tuple2<Integer, Integer>> map : maps) {
            if (map.containsKey(key)) {
                tfSum += map.get(key).a;
            }
        }
        return tfSum;
    }

    public static <T extends Object> TObjectIntMap<T>[] loadMaps(String dbPath, String[] dbNames, String mapName, Logger logger, String msg) throws Exception {
        TObjectIntMap<T>[] maps = new TObjectIntMap[dbNames.length];
        for (int i = 0; i < dbNames.length; i++) {
            String mapPath = new File(dbPath, dbNames[i] + "_" + mapName).getAbsolutePath();
            logger.info(msg + " " + mapPath);
            TObjectIntMap<T> map = (TObjectIntMap<T>) SerializationHelper.read(mapPath);
            maps[i] = map;
        }
        return maps;
    }

    public static Pair<Integer, Integer> getCounts(MooneyEventRepre former, MooneyEventRepre latter,
                                                   TObjectIntMap<TIntList>[] cooccCountMaps, TObjectIntMap<TIntList>[] occCountMaps, TObjectIntMap<String>[] headIdMaps) {
        int coocCount = 0;
        int occCount = 0;

        for (int i = 0; i < cooccCountMaps.length; i++) {
            TObjectIntMap<String> headIdMap = headIdMaps[i];
            TIntLinkedList formerTuple = former.toCompactForm(headIdMap);
            TIntLinkedList latterTuple = latter.toCompactForm(headIdMap);

            TIntLinkedList joinedPair = MooneyEventRepre.joinCompactForm(formerTuple, latterTuple);

            TObjectIntMap<TIntList> cooccCountMap = cooccCountMaps[i];
            if (cooccCountMap.containsKey(joinedPair)) {
                coocCount += cooccCountMap.get(joinedPair);
            }

            TObjectIntMap<TIntList> occCountMap = occCountMaps[i];
            if (occCountMap.containsKey(formerTuple)) {
                occCount += occCountMap.get(formerTuple);
            }
        }

        return Pair.of(occCount, coocCount);
    }
}
