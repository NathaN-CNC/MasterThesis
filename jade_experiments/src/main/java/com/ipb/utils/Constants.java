package com.ipb.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Constants {
    public static final boolean FILTER_SURES = true;
    public static final boolean CHECK_ASK_OPINION_COUNT = true;
    public static final boolean CHECK_AVERAGE_API_REQUEST_TIME = false;

    public static final HashMap<String, String> SKILL_URLS = new HashMap<String, String>() {{
        put("randomforest", "http://localhost:5000");
        put("MLP", "http://localhost:5001");
        put("LogisticReg", "http://localhost:5002");
        put("KNN", "http://localhost:5003");
        put("DecisionTree", "http://localhost:5004");
        put("SVM", "http://localhost:5005");
    }};

    public static final HashMap<String, Long> SKILL_PERIODS = new HashMap<String, Long>() {{
        put("randomforest", 80L);
        put("MLP", 80L);
        put("LogisticReg", 80L);
        put("KNN", 80L);
        put("DecisionTree", 80L);
        put("SVM", 80L);
    }};

    public static final List<String> SKILLS = new ArrayList<String>(SKILL_URLS.keySet());
    public static final String TEST_PATH = "C:\\Users\\natha\\Documents\\AgentesML\\python\\dadosteste2.csv";
    public static final String TRAIN_PATH = "C:\\Users\\natha\\Documents\\AgentesML\\python\\dadostreino.csv";

    public static final float[] SAMPLE_DATA = {
            0.810562f, 10, 6, 498, 268,
            18.505678f, 254, 252, 4431.493164f, 2210.811768f, 2, 1, 84.232444f,
            130.586398f, 4710.843969f, 191.144969f, 255, 2465714369f, 4011849693f, 255,
            0.208918f, 0.157627f, 0.051291f, 50, 45, 0, 0, 6, 1, 3, 3, 3, 5, 0, 0, 0, 5,
            5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0
    };
}
