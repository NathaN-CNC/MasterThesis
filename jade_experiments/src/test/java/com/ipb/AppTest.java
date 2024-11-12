package com.ipb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;

import com.ipb.agents.OpinionResult;
import com.ipb.utils.Constants;


public class AppTest {
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void getBestSkill() {
        Map<String, Long> pointsPerSkill = new HashMap<String, Long>()
        {
            {
                put("RF", 3L);
                put("SVM", 10L);
                put("KNN", 5L);
            }
        };

        Map.Entry<String, Long>[] points = (Map.Entry<String, Long>[]) pointsPerSkill.entrySet().toArray(new Map.Entry[0]);
        Arrays.sort(points, (a, b) -> Long.compare(b.getValue(), a.getValue()));
        assertEquals(points[0].getKey(), "SVM");
        assertEquals(points[1].getKey(), "KNN");
        assertEquals(points[2].getKey(), "RF");
    }
    
    @Test
    public void indexOfEqualsFor() {
        String mySkill = "SVM";
        int indexFor = 0;
        for (String skill : Constants.SKILLS) {
            if (skill.equals(mySkill)) {
                break;
            }
            indexFor++;
        }
        int indexOf = Constants.SKILLS.indexOf(mySkill);
        assertEquals(indexFor, indexOf);
    }

    @Test
    public void filterOpinionResultUp() {
        OpinionResult r = new OpinionResult(null, null);
        r.addOpinion(0.6f);

        r.addOpinion(0.7f);
        r.addOpinion(0.55f);
        r.addOpinion(0.45f);
        r.addOpinion(0.3f);
        List<Float> isAttackList = r.getIsAttackList();
        assertEquals(isAttackList.size(), 3);
        assertEquals(isAttackList.get(0), 0.6f, 0.01);
        assertEquals(isAttackList.get(1), 0.7f, 0.01);
        assertEquals(isAttackList.get(2), 0.3f, 0.01);
        assertEquals(r.voteIsAttack(), 1.0, 0.01);
    }

    @Test
    public void filterOpinionResultDown() {
        OpinionResult r = new OpinionResult(null, null);
        r.addOpinion(0.4f);

        r.addOpinion(0.7f);
        r.addOpinion(0.55f);
        r.addOpinion(0.45f);
        r.addOpinion(0.3f);
        List<Float> isAttackList = r.getIsAttackList();
        assertEquals(isAttackList.size(), 3);
        assertEquals(isAttackList.get(0), 0.4f, 0.01);
        assertEquals(isAttackList.get(1), 0.7f, 0.01);
        assertEquals(isAttackList.get(2), 0.3f, 0.01);
        assertEquals(r.voteIsAttack(), 0.0, 0.01);
    }
}
