package com.ipb.agents;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.ipb.behaviours.MessageListenerBehaviour;
import com.ipb.behaviours.ProcessIteratorTickerBehaviour;
import com.ipb.message.MetricsResponseMessage;
import com.ipb.message.OpinionRequestMessage;
import com.ipb.message.OpinionResponseMessage;
import com.ipb.message.PointsRequestMessage;
import com.ipb.message.PointsResponseMessage;
import com.ipb.message.ReadyMessage;
import com.ipb.utils.AgentExtension;
import com.ipb.utils.AgentLogger;
import com.ipb.utils.ApiModel;
import com.ipb.utils.Constants;
import com.ipb.utils.LoadCsvFromDisk;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgentAI2 extends Agent {
    // Constants
    public final long OPINION_TIMEOUT = 5_000; // milliseconds
    public final long WINDOW_METRICS = 5_000; // milliseconds
    private static String USAGE = "Usage <agent-name>:com.ipb.agents.AgentAI2(<agent-skill>)\n  agent-skill: RandomForest | ExtraTrees | XGBoost\n  Example: AgentAI:com.ipb.agents.AgentAI(RandomForest)";

    // Setup
    private AgentLogger logger = new AgentLogger(this);
    private String skill;
    private ApiModel api;
    private Integer skip = null;
    private Integer limit = null;
    private float confidenceThreasholdDown = 0.2f;
    private float confidenceThreasholdUp = 0.8f;

    // State
    private ConcurrentHashMap<UUID, OpinionResult> opinions = new ConcurrentHashMap<UUID, OpinionResult>();
    private AtomicInteger counter = new AtomicInteger(0);
    private long prevCount = 0;

    private Metrics metricsSelfWindow = new Metrics("self-time-window");
    private Metrics metricsAsk = new Metrics("ask");
    private Metrics metricsModelSelf = new Metrics("self-model");
    private Metrics metricsModelAll = new Metrics("all-model");
    private Metrics metricsModelSelfWindow = new Metrics("self-count-window-model");
    private Metrics metricsSelf = new Metrics("self");
    private Metrics metricsAll = new Metrics("all");

    private ProcessIteratorTickerBehaviour<float[]> processor;

    @Override
    protected void setup() {
        if (!setupSkill()) {
            doDelete();
            return;
        }
        setupBoundaries();

        addBehaviour(createBehaviourInformListener());
    }

    @Override
    protected void takeDown() {
        AgentExtension.deregisterSkill(this);
        System.out.println("Agent " + getLocalName() + " is shutting down");
    }

    private Behaviour createBehaviourInformListener() {
        return new MessageListenerBehaviour(this, MessageTemplate.MatchPerformative(ACLMessage.INFORM)) {
            @Override
            protected void dispatchObjectMessage(ACLMessage message, Serializable contentObject) {
                if (contentObject instanceof ReadyMessage) {
                    logger.info("RECEIVE(READY)");
                    addBehaviour(createProcessIteratorBehaviour());
                    addBehaviour(createWindowMetricsBehavior());

                } else if (contentObject instanceof OpinionResponseMessage) {
                    logger.info("RECEIVE(OPINION_RESPONSE)");
                    OpinionResponseMessage opinionRequest = (OpinionResponseMessage) contentObject;
                    addBehaviour(createBehaviourAnalyzeOptionResponses(opinionRequest)); // response

                } else if (contentObject instanceof OpinionRequestMessage) {
                    logger.info("RECEIVE(OPINION_REQUEST)");
                    OpinionRequestMessage opinionRequest = (OpinionRequestMessage) contentObject;
                    addBehaviour(createBehaviourAnswerOpinionRequests(opinionRequest, message.getSender())); // request

                } else if (contentObject instanceof PointsResponseMessage) {
                    logger.info("RECEIVE(POINTS_RESPONSE)");
                    PointsResponseMessage pointsResponse = (PointsResponseMessage) contentObject;
                    addBehaviour(createBehaviourAnalyzePointsResponse(pointsResponse));
                    
                } else {
                    logger.warn("RECEIVE(UNKNOWN) : " + contentObject);
                }
            }
        };
    }

    private void sendMetrics(boolean force) {
        if (force || (metricsAll.getTotalCount()) % 100 == 0) {
            addBehaviour(createBehaviourSendMetrics(metricsAll.getName(), metricsAll));
        }
        if (force || (metricsSelf.getTotalCount()) % 100 == 0) {
            addBehaviour(createBehaviourSendMetrics(metricsSelf.getName(), metricsSelf));
            addBehaviour(createBehaviourSendMetrics(metricsAsk.getName(), metricsAsk));
        }
        if (force || (metricsModelSelf.getTotalCount()) % 100 == 0) {
            addBehaviour(createBehaviourSendMetrics(metricsModelAll.getName(), metricsModelAll));
            addBehaviour(createBehaviourSendMetrics(metricsModelSelf.getName(), metricsModelSelf));
            addBehaviour(createBehaviourSendMetrics(metricsModelSelfWindow.getName(), metricsModelSelfWindow));
        }
    }

    private Behaviour createProcessIteratorBehaviour() {
        Iterator<float[]> dataIterator;
        try {
            Stream<float[]> dataStream = LoadCsvFromDisk.stream(Constants.TEST_PATH);

            if (skip != null) {
                dataStream = dataStream.skip(skip);
            }
            if (limit != null) {
                dataStream = dataStream.limit(limit);
            }

            dataIterator = dataStream.iterator();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        processor = new ProcessIteratorTickerBehaviour<float[]>(this, AgentExtension.getPeriod(skill), dataIterator) {
            @Override
            protected void processItem(float[] item) {
                analyzeData(item);
            }

            @Override
            protected void onDone() {
                sendMetrics(true);
            }
        };

        return processor;
    }

    private Behaviour createWindowMetricsBehavior() {
        return new TickerBehaviour(this, WINDOW_METRICS) {
            @Override
            public void onStart() {
                super.onStart();
                setFixedPeriod(true);
            }

            @Override
            protected void onTick() {
                if (processor == null) {
                    return;
                }

                if (processor.done()) {
                    stop();
                    return;
                }

                addBehaviour(createBehaviourSendMetrics("self-window", metricsSelfWindow));
                metricsSelfWindow = metricsSelfWindow.reset();
            }
        };
    }

    private float[] apiPredict(float[] inputData) {
        Instant start = Instant.now();
        float[] response = api.predict(inputData);
        Instant end = Instant.now();
        float apiRequestTime = (float) (end.toEpochMilli() - start.toEpochMilli()) / 1000;
        metricsSelf.updateApiRequestTime(apiRequestTime);
        metricsModelSelf.updateApiRequestTime(apiRequestTime);
        metricsSelfWindow.updateApiRequestTime(apiRequestTime);
        metricsModelSelfWindow.updateApiRequestTime(apiRequestTime);
        return response;
    }

    private void analyzeData(float[] inputData) {
        float[] response = apiPredict(inputData);
        Float isAttackProb = response[1];
        boolean sure = isAttackProb >= confidenceThreasholdUp || isAttackProb <= confidenceThreasholdDown;
        // TODO: Resolve conflicts
        int count = counter.incrementAndGet();
        UUID id = UUID.randomUUID();
        boolean isAttack = isAttackProb >= 0.5;
        float isReallyAttackProb = inputData[inputData.length - 1];
        boolean isReallyAttack = isReallyAttackProb >= 0.5;
        this.logger.info("ANALYZE(" + id + ")"
                + " count=" + count
                + " sure=" + sure
                + " prob=" + isAttackProb
                + " label=" + isReallyAttack
                + " correct=" + (isAttack == isReallyAttack));

        OpinionResult result = new OpinionResult(id, inputData);
        result.addOpinion(isAttackProb);

        metricsSelf.update(isAttackProb, isReallyAttackProb);
        metricsModelSelf.update(isAttackProb, isReallyAttackProb);
        metricsSelfWindow.update(isAttackProb, isReallyAttackProb);
        metricsModelSelfWindow.update(isAttackProb, isReallyAttackProb);

        if ((count + 1) % 500 == 0) {
            addBehaviour(createBehaviourAnalyzeMetrics());
        }

        if (metricsModelSelfWindow.getTotalCount() % 500 == 0) {
            metricsModelSelfWindow = metricsModelSelfWindow.reset();
        }

        sendMetrics(false);

        if (sure) {
            addBehaviour(createBehaviourAnalyzeResult(result));
        } else {
            metricsSelf.incrementAskOpinionCount();
            metricsModelSelf.incrementAskOpinionCount();
            metricsSelfWindow.incrementAskOpinionCount();
            metricsModelSelfWindow.incrementAskOpinionCount();

            /// Passa o valor entre 20 e 80 para o objetod result // result pode ser opiniao
            opinions.put(id, result); // cria um dicionario e passa pra ele a chave e o valor

            SequentialBehaviour s = new SequentialBehaviour();
            s.addSubBehaviour(createBehaviourAskForOpinion(result));
            s.addSubBehaviour(createBehaviourTimeout(id, inputData));
            addBehaviour(s);
        }
    }

    private Behaviour createBehaviourAnalyzeMetrics() {
        return new OneShotBehaviour() {
            @SuppressWarnings("unused")
            @Override
            public void action() {
                if (Constants.CHECK_ASK_OPINION_COUNT && checkAskOpinionCount()) {
                    return;
                }

                if (Constants.CHECK_AVERAGE_API_REQUEST_TIME && checkAverageApiRequestTime()) {
                    return;
                }
            }

            private boolean checkAverageApiRequestTime() {
                logger.info("ANALYZE(METRICS) : API time=" + metricsModelSelf.getAverageApiRequestTime());

                float averageApiRequestTimeThreshold = 0.1f; // seconds

                if (metricsModelSelf.getAverageApiRequestTime() > averageApiRequestTimeThreshold) {
                    logger.warn("ANALYZE(METRICS) : average api request time is high: "
                            + metricsModelSelf.getAverageApiRequestTime());
                    addBehaviour(createBehaviourTryChangeSkill());
                    return true;
                }

                return false;
            }

            private boolean checkAskOpinionCount() {
                logger.info("ANALYZE(METRICS) : Ask count=" + metricsModelSelfWindow.getAskOpinionCount());

                long askOpinionCountThreshold = 100;

                if (metricsModelSelfWindow.getAskOpinionCount() > askOpinionCountThreshold) {
                    logger.warn("ANALYZE(METRICS) : ask opinion count is high: " + metricsModelSelfWindow.getAskOpinionCount());
                    addBehaviour(createBehaviourTryChangeSkill());
                    return true;
                }

                return false;
            }
        };
    }

    @SuppressWarnings("unused")
    private Behaviour createBehaviorTryChangeSkillRoundRobin() {
        return new OneShotBehaviour() {
            @Override
            public void action() {
                int index = Constants.SKILLS.indexOf(skill);
                String newSkill = Constants.SKILLS.get((index + 1) % Constants.SKILLS.size());
                changeSkill(newSkill);
            }
        };
    }

    private Behaviour createBehaviourTryChangeSkill() {
        return new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                AgentExtension.setTrustyContentObject(msg, new PointsRequestMessage());
                msg.setEncoding("UTF-8");

                try {
                    DFAgentDescription desc = new DFAgentDescription();
                    ServiceDescription service = new ServiceDescription();
                    service.setType("metrics");
                    desc.addServices(service);
                    DFAgentDescription[] agentDescs = DFService.search(getAgent(), desc);
                    for (DFAgentDescription agentDesc : agentDescs) {
                        msg.addReceiver(new AID(agentDesc.getName().getLocalName(), AID.ISLOCALNAME));
                    }

                    if (agentDescs.length > 0) {
                        send(msg);
                        logger.info("CHANGE_SKILL : searching for metrics agents");
                    } else {
                        logger.error("CHANGE_SKILL : failed to search for metrics agents");
                        doDelete();
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                    logger.error("CHANGE_SKILL : failed to search for metrics agents");
                    doDelete();
                    return;
                }
            }
        };
    }

    private Behaviour createBehaviourAnalyzePointsResponse(PointsResponseMessage pointsResponse) {
        return new OneShotBehaviour() {
            @SuppressWarnings("unchecked")
            @Override
            public void action() {
                Map<String, Long> pointsPerSkill = pointsResponse.getPointsPerSkill();
                Map.Entry<String, Long>[] points = (Map.Entry<String, Long>[]) pointsPerSkill.entrySet().toArray(new Map.Entry[0]);
                Arrays.sort(points, (a, b) -> Long.compare(b.getValue(), a.getValue()));

                String bestSkill = points.length > 0 ? points[0].getKey() : null;

                if (bestSkill == null) {
                    logger.warn("ANALYZE(POINTS_RESPONSE) : failed to find best skill");
                    return;
                }

                if (bestSkill.equals(skill)) {
                    logger.info("ANALYZE(POINTS_RESPONSE) : best skill is the same: " + bestSkill);
                    return;
                }

                logger.info("ANALYZE(POINTS_RESPONSE) : best skill is different: " + bestSkill);
                changeSkill(bestSkill);
            }
        };
    }

    private void changeSkill(String newSkill) {
        AgentExtension.deregisterSkill(this);

        logger.info("TRY_CHANGE_SKILL : " + skill + " -> " + newSkill);
        skill = newSkill;
        api = AgentExtension.getModel(skill);
        processor.reset(AgentExtension.getPeriod(skill));
        metricsModelSelf = metricsModelSelf.reset();
        metricsModelAll = metricsModelAll.reset();
        metricsModelSelfWindow = metricsModelSelfWindow.reset();


        if (api == null) {
            logger.error("Invalid learning model" + skill + "\n" + USAGE);
            doDelete();
            return;
        }

        if (!AgentExtension.registerSkill(this, skill)) {
            logger.error("Failed to register skill" + skill + "\n" + USAGE);
            doDelete();
            return;
        }
    }

    private Behaviour createBehaviourAnswerOpinionRequests(OpinionRequestMessage request, AID sender) {
        return new OneShotBehaviour(this) {
            @Override
            public void action() {
                UUID id = request.getId();
                float[] inputData = request.getInputData();
                float[] response = apiPredict(inputData);
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                AgentExtension.setTrustyContentObject(msg,
                        new OpinionResponseMessage(id, response, request.getStartTime()));
                msg.addReceiver(sender);
                send(msg);

                float isAttackProb = response[1];
                float isReallyAttackProb = inputData[inputData.length - 1];
                metricsAsk.update(isAttackProb, isReallyAttackProb);
                logger.info("ANSWER(OPINION) : " + id + " : " + sender.getLocalName());
            }
        };
    }

    private Behaviour createBehaviourAnalyzeOptionResponses(OpinionResponseMessage response) {
        return new OneShotBehaviour(this) {
            @Override
            public void action() {
                UUID id = response.getId();
                OpinionResult result = opinions.get(id);

                if (result == null) {
                    // Timeout
                    logger.warn("ANALYZE(" + id + ") : received opinion response without a request");
                    return;
                }

                metricsAll.updateTimePerAsk(response.getTotalTime());
                metricsSelf.updateTimePerAsk(response.getTotalTime());
                metricsModelSelf.updateTimePerAsk(response.getTotalTime());
                metricsSelfWindow.updateTimePerAsk(response.getTotalTime());
                metricsModelAll.updateTimePerAsk(response.getTotalTime());
                metricsModelSelfWindow.updateTimePerAsk(response.getTotalTime());

                float isAttack = response.getOutputData()[1];
                result.addOpinion(isAttack);
                logger.info("ANALYZE(" + id + ") : received opinion response: " + isAttack + " : "
                        + result.getIsAttackList().size());

                if (result.isComplete()) {
                    logger.info("ANALYZE(" + id + ") : opinion complete: " + result.getIsAttackList());
                    opinions.remove(id);
                    addBehaviour(createBehaviourAnalyzeResult(result));

                    metricsAll.updateTimePerVoting(response.getTotalTime());
                    metricsSelf.updateTimePerVoting(response.getTotalTime());
                    metricsModelSelf.updateTimePerVoting(response.getTotalTime());
                    metricsSelfWindow.updateTimePerVoting(response.getTotalTime());
                    metricsModelAll.updateTimePerVoting(response.getTotalTime());
                    metricsModelSelfWindow.updateTimePerVoting(response.getTotalTime());
                }
            }
        };
    }

    private Behaviour createBehaviourAnalyzeResult(OpinionResult result) {
        return new OneShotBehaviour(this) {
            @Override
            public void action() {
                UUID id = result.getId();
                float[] inputData = result.getInputData();
                float isAttackProb = result.voteIsAttack();
                float isReallyAttackProb = inputData[inputData.length - 1];

                metricsAll.update(isAttackProb, isReallyAttackProb);
                metricsModelAll.update(isAttackProb, isReallyAttackProb);

                boolean isAttack = isAttackProb >= 0.5;
                boolean isReallyAttack = isReallyAttackProb >= 0.5;
                logger.info(
                        "RESULT(" + id + ") : result is attack? " + isAttack + " : prob? " + isAttackProb + " : label? "
                                + isReallyAttack
                                + " : correct? " + (isAttack == isReallyAttack));
            }
        };
    }

    private Behaviour createBehaviourAskForOpinion(OpinionResult result) {
        return new OneShotBehaviour() {
            @Override
            public void action() {
                String mySkill = skill;
                int count = 0;
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                AgentExtension.setTrustyContentObject(msg,
                        new OpinionRequestMessage(result.getId(), result.getInputData(), Instant.now()));
                // msg.setConversationId(id.toString());

                for (String skill : Constants.SKILLS) {
                    if (!skill.equals(mySkill)) { // dif da skill atual
                        DFAgentDescription desc = new DFAgentDescription();
                        ServiceDescription service = new ServiceDescription();
                        service.setType(skill); // seta o tipo de serviço / skill
                        desc.addServices(service); // seta a descrição como tipo de serviço
                        try {
                            // retorna a descrição dos agentes que correspondem aos critérios de busca
                            DFAgentDescription[] agentDescs = DFService.search(getAgent(), desc);
                            for (DFAgentDescription agentDesc : agentDescs) {
                                msg.addReceiver(new AID(agentDesc.getName().getLocalName(), AID.ISLOCALNAME));
                                count++;
                            }
                        } catch (FIPAException e) {
                            e.printStackTrace();
                        }
                    }
                }
                send(msg);
                logger.info("ASK(" + result.getId() + ") : agents=" + count + "");
                result.setup(count);
                prevCount = count;
            }
        };
    }

    private Behaviour createBehaviourTimeout(UUID id, float[] inputData) {
        return new WakerBehaviour(this, OPINION_TIMEOUT + 1000 * (prevCount - 1)) {
            @Override
            protected void onWake() {
                OpinionResult result = opinions.remove(id);
                if (result != null) {
                    metricsSelf.incrementTimeoutCount();
                    metricsModelSelf.incrementTimeoutCount();
                    metricsSelfWindow.incrementTimeoutCount();
                    metricsModelSelfWindow.incrementTimeoutCount();
                    logger.info("TIMEOUT(Y) : " + id + " : " + result.getIsAttackList());
                    addBehaviour(createBehaviourAnalyzeResult(result));
                } else {
                    logger.info("TIMEOUT(N) : " + id);
                }
            }
        };
    }

    protected Behaviour createBehaviourSendMetrics(String type, Metrics metrics) {
        return new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    int count = 0;
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    String name = getLocalName();
                    msg.setContentObject(new MetricsResponseMessage(name, skill, type, metrics));
                    msg.setEncoding("UTF-8");
                    DFAgentDescription desc = new DFAgentDescription();
                    ServiceDescription service = new ServiceDescription();
                    service.setType("metrics"); // seta o tipo de serviço / skill
                    desc.addServices(service); // seta a descrição como tipo de serviço
                    try {
                        // retorna a descrição dos agentes que correspondem aos critérios de busca
                        DFAgentDescription[] agentDescs = DFService.search(getAgent(), desc);
                        for (DFAgentDescription agentDesc : agentDescs) {
                            msg.addReceiver(new AID(agentDesc.getName().getLocalName(), AID.ISLOCALNAME));
                            count++;
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                    if (count > 0) {
                        send(msg);
                        logger.info("SEND(METRICS_REQUEST): type=" + type + " count=" + count);
                    } else {
                        logger.warn("SEND(METRICS_REQUEST): type=" + type + " count=" + count);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private boolean setupSkill() {
        Object[] args = getArguments();
        if (args.length < 1) {
            this.logger.error("Invalid number of arguments\n" + USAGE);
            return false;
        }

        this.skill = args[0].toString();
        this.api = AgentExtension.getModel(this.skill);

        /** Verificar se a skill passada é valida */

        if (this.api == null) {
            this.logger.error("Invalid learning model" + this.skill + "\n" + USAGE);
            return false;
        }

        if (!AgentExtension.registerSkill(this, this.skill)) {
            this.logger.error("Failed to register skill" + this.skill + "\n" + USAGE);
            return false;
        }

        this.logger.info("Skill " + this.skill + " registered");

        return true;
    }

    private int setNamedArg(Object[] args, String key, int index) {
        if (key.equals("skip")) {
            skip = Integer.parseInt(args[index].toString());
            return index + 2;
        } else if (key.equals("limit")) {
            limit = Integer.parseInt(args[index].toString());
            return index + 2;
        } else if (key.equals("confidence")) {
            confidenceThreasholdUp = Float.parseFloat(args[index].toString());
            confidenceThreasholdDown = 1.0f - confidenceThreasholdUp;
            return index + 2;
        } else if (key.startsWith("skip=")) {
            skip = Integer.parseInt(key.substring(5));
            return index + 1;
        } else if (key.startsWith("limit=")) {
            limit = Integer.parseInt(key.substring(6));
            return index + 1;
        } else if (key.startsWith("confidence=")) {
            confidenceThreasholdUp = Float.parseFloat(key.substring(11));
            confidenceThreasholdDown = 1.0f - confidenceThreasholdUp;
            return index + 1;
        } else {
            logger.warn("Unknown argument: " + key);
            return index + 1;
        }
    }

    private void setupBoundaries() {
        Object[] args = getArguments();
        int argc = 2;
        while (argc <= args.length) {
            argc = setNamedArg(args, args[argc - 1].toString(), argc);
        }
        logger.info("Boundaries: skip=" + skip + " limit=" + limit);
    }
}
