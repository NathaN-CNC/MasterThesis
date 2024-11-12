package com.ipb.agents;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.ipb.message.OpinionRequestMessage;
import com.ipb.message.OpinionResponseMessage;
import com.ipb.message.ReadyMessage;
import com.ipb.utils.AgentExtension;
import com.ipb.utils.AgentLogger;
import com.ipb.utils.ApiModel;
import com.ipb.utils.Constants;
import com.ipb.utils.LoadCsvFromDisk;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class AgentAI extends Agent {
    // Constants
    public final float CONFIDENCE_THREASHOLD_UP = 0.8f;
    public final float CONFIDENCE_THREASHOLD_DOWN = 0.2f;
    public final long OPINION_TIMEOUT = 5_000; // milliseconds
    private static String USAGE = "Usage <agent-name>:com.ipb.agents.AgentAI(<agent-skill>)\n  agent-skill: RandomForest | ExtraTrees | XGBoost\n  Example: AgentAI:com.ipb.agents.AgentAI(RandomForest)";

    // Setup
    private AgentLogger logger = new AgentLogger(this);
    private String skill;
    private ApiModel api;

    // State
    private ConcurrentHashMap<UUID, OpinionResult> bestOpinions = new ConcurrentHashMap<UUID, OpinionResult>();
    private AtomicInteger counter = new AtomicInteger(0);
    private Metrics metrics = new Metrics("all");

    protected void setup() {
        Object[] args = getArguments();
        if (args.length != 1) {
            this.logger.error("Invalid number of arguments\n" + USAGE);
            this.doDelete();
            return;
        }

        this.skill = args[0].toString();
        this.api = AgentExtension.getModel(this.skill);

        /** Verificar se a skill passada é valida */

        if (this.api == null) {
            this.logger.error("Invalid learning model: " + this.skill + "\n" + USAGE);
            this.doDelete();
            return;
        }

        if (!AgentExtension.registerSkill(this, this.skill)) {
            this.logger.error("Failed to register skill: " + this.skill + "\n" + USAGE);
            this.doDelete();
            return;
        }

        System.out.println("Learning model loaded: " + this.skill);

        addBehaviour(createBehaviourMessagesListener());

        System.out.println("Agent " + getLocalName() + " is ready");
    }

    protected void takeDown() {
        AgentExtension.deregisterSkill(this);
        System.out.println("Agent " + getLocalName() + " is shutting down");
    }

    /*
     * O método itera sobre as habilidades disponíveis definidas na classe
     * Constants.SKILLS. Para cada habilidade que não corresponda à habilidade do
     * agente atual (this.skill),
     * uma solicitação de opinião é enviada para os agentes que oferecem essa
     * habilidade.
     * Cria uma descrição de agente (DFAgentDescription) para procurar agentes que
     * ofereçam o tipo de serviço correspondente à habilidade atual.
     * Para cada agente encontrado, cria uma mensagem ACL (Agent Communication
     * Language) do tipo ACLMessage.INFORM, adiciona o agente como receptor da
     * mensagem,
     * define o conteúdo da mensagem como uma solicitação de opinião usando um
     * objeto OpinionMessage, que parece ser uma classe que encapsula a solicitação
     * de opinião com
     * o identificador único e os dados de entrada.
     * Depois de enviar a mensagem, incrementa o contador de solicitações enviadas e
     * imprime uma mensagem de registro no console para acompanhar a solicitação de
     * opinião.
     * Esse método é responsável por iniciar o processo de solicitação de opinião a
     * outros agentes para um determinado resultado de análise representado por
     * OpinionResult.
     * Ele utiliza o mecanismo de busca do DFService para encontrar agentes que
     * oferecem habilidades específicas e envia solicitações de opinião para eles.
     */

    private void askForOpinion(OpinionResult result) {
        int count = 0;
        Date replyByDate = new Date(System.currentTimeMillis() + OPINION_TIMEOUT);
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        // msg.setConversationId(id.toString());
        msg.setReplyByDate(replyByDate);
        try {
            msg.setContentObject(new OpinionRequestMessage(result.getId(), result.getInputData(), Instant.now()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("unreachable code");
        }

        for (String skill : Constants.SKILLS) {
            if (!skill.equals(this.skill)) { // dif da skill atual
                DFAgentDescription desc = new DFAgentDescription();
                ServiceDescription service = new ServiceDescription();
                service.setType(skill); // seta o tipo de serviço / skill
                desc.addServices(service); // seta a descrição como tipo de serviço
                try {
                    // retorna a descrição dos agentes que correspondem aos critérios de busca
                    DFAgentDescription[] agentDescs = DFService.search(this, desc);
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
        this.logger.info("ASK(" + result.getId() + ") : asked for opinion to " + count + " agents");
        result.setup(count);
    }

    /*
     * Os dados de entrada são analisados por um modelo de IA e, se a probabilidade
     * de ataque
     * estiver dentro de determinados limites, a opinião de outros agentes é
     * solicitada para ajudar na tomada de decisão.
     */
    private void analyzeData(float[] inputData) {
        float[] response = api.predict(inputData);
        Float isAttackProb = response[1];
        boolean sure = isAttackProb >= CONFIDENCE_THREASHOLD_UP || isAttackProb <= CONFIDENCE_THREASHOLD_DOWN;
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

        metrics.update(isAttackProb, isReallyAttackProb);

        if (sure) {
            addBehaviour(createBehaviorAnalyzeResult(id, inputData, isAttackProb));
        } else {
            OpinionResult result = new OpinionResult(id, inputData);
            /// Passa o valor entre 20 e 80 para o objetod result // result pode ser opiniao
            result.addOpinion(isAttackProb);
            bestOpinions.put(id, result); // cria um dicionario e passa pra ele a chave e o valor

            SequentialBehaviour s = new SequentialBehaviour();
            s.addSubBehaviour(createBehaviorAskForOpinion(result));
            s.addSubBehaviour(createBehaviourTimeout(id, inputData));
            addBehaviour(s);
        }
    }

    /*
     * analisar as respostas de opinião recebidas de outros agentes em relação a uma
     * solicitação específica identificada por id.
     */
    private void answerOpinionRequests(OpinionRequestMessage request, AID otherAgent) {
        float[] response = api.predict(request.getInputData());

        try {
            UUID id = request.getId();
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(otherAgent);
            msg.setContentObject(new OpinionResponseMessage(id, response, request.getStartTime()));
            send(msg);
            this.logger.info("ANSWER(OPINION) : " + id + " : " + otherAgent.getLocalName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * analisa as respostas de opinião recebidas de outros agentes em relação a uma
     * solicitação específica identificada por um id.
     */
    private void analyzeOptionResponses(UUID id, float[] outputData) {
        OpinionResult result = bestOpinions.get(id);
        if (result == null) {
            // Timeout
            this.logger.warn("ANALYZE(" + id + ") : received opinion response without a request");
            return;
        }

        float isAttack = outputData[1];
        result.addOpinion(isAttack);
        this.logger.info("ANALYZE(" + id + ") : received opinion response: " + isAttack + " : "
                + result.getIsAttackList().size());

        if (result.isComplete()) {
            this.logger.info("ANALYZE(" + id + ") : opinion complete: " + result.getIsAttackList());
            bestOpinions.remove(id);
            float opinionsIsAttack = result.averageIsAttack();
            analyzeResult(id, result.getInputData(), opinionsIsAttack);
        }
    }

    /*
     * analisar o resultado da predição da IA sobre os dados de entrada e imprimir
     * uma mensagem no console indicando se o ataque foi detectado ou não.
     */
    private void analyzeResult(UUID id, float[] inputData, float isAttackProb) {
        boolean isAttack = isAttackProb >= 0.5;
        boolean isReallyAttack = inputData[inputData.length - 1] >= 0.5;
        this.logger.info(
                "RESULT(" + id + ") : result is attack? " + isAttack + " : prob? " + isAttackProb + " : label? "
                        + isReallyAttack
                        + " : correct? " + (isAttack == isReallyAttack));
    }

    /*
     * Como estamos lidando com segurança devemos 'limitar' o agente a escutar
     * apenas alguns tipos de mensagens,
     * Para selecionar as mensagens que um agente deseja receber podemos utilizar a
     * classe jade.lang.acl.MessageTemplate.
     * Esta classe permite definir filtros para cada atributo da mensagem ACLMessage
     * e estes filtros podem ser utilizados
     * como parâmetros do método receive().Nesta classe se define um conjunto de
     * métodos estáticos que retornam como resultado
     * um objeto do tipo MessageTemplate.O nosso agente Robo poderia estar
     * interessado em receber apenas mensagens do tipo inform
     * e cuja linguagem seja o Português.
     */
    private Behaviour createBehaviourMessagesListener() {
        return new CyclicBehaviour(this) {
            @Override
            public void action() {
                // ACLMessage msg = receive();
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                System.out.println("Message Queue Size: " + getCurQueueSize() + " / " + getQueueSize());
                if (msg == null) {
                    logger.warn("RECEIVE(NULL)");
                    block();
                    return;
                }

                Serializable contentObject = null;
                try {
                    contentObject = msg.getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }

                if (contentObject == null) {
                    logger.warn("RECEIVE(NULL) : " + msg.getContent());
                    block();
                    return;
                }

                dispatchObjectMessage(contentObject, msg);
                block();
            }

            private void dispatchObjectMessage(Serializable contentObject, ACLMessage msg) {
                if (contentObject instanceof ReadyMessage) {
                    logger.info("RECEIVE(READY)");
                    addBehaviour(createBehaviourStartProcessing());
                } else if (contentObject instanceof OpinionResponseMessage) {
                    logger.info("RECEIVE(OPINION_RESPONSE)");
                    OpinionResponseMessage opinionRequest = (OpinionResponseMessage) contentObject;
                    addBehaviour(createBehaviourAnalyzeOptionResponses(opinionRequest.getId(),
                            opinionRequest.getOutputData())); // response
                } else if (contentObject instanceof OpinionRequestMessage) {
                    logger.info("RECEIVE(OPINION_REQUEST)");
                    OpinionRequestMessage opinionRequest = (OpinionRequestMessage) contentObject;
                    addBehaviour(createBejaviorAnswerOpinionRequests(opinionRequest, msg.getSender())); // request
                } else {
                    logger.warn("RECEIVE(UNKNOWN) : " + contentObject);
                }
            }
        };
    }

    protected Behaviour createBehaviorAnalyzeResult(UUID id, float[] inputData, float isAttackProb) {
        return new OneShotBehaviour(this) {
            @Override
            public void action() {
                analyzeResult(id, inputData, isAttackProb);
            }
        };
    }

    private Behaviour createBehaviorAskForOpinion(OpinionResult result) {
        return new OneShotBehaviour() {
            @Override
            public void action() {
                askForOpinion(result);
            }
        };
    }

    protected Behaviour createBehaviourAnalyzeOptionResponses(UUID id, float[] outputData) {
        return new OneShotBehaviour(this) {
            @Override
            public void action() {
                analyzeOptionResponses(id, outputData);
            }
        };
    }

    protected Behaviour createBejaviorAnswerOpinionRequests(OpinionRequestMessage request, AID sender) {
        return new OneShotBehaviour(this) {
            @Override
            public void action() {
                answerOpinionRequests(request, sender);
            }
        };
    }

    private Behaviour createBehaviourStartProcessing() {
        return new OneShotBehaviour(this) {
            @Override
            public void action() {
                // TODO: Create an agent to send the data
                try {
                    List<float[]> data = LoadCsvFromDisk
                            .stream(Constants.TEST_PATH)
                            // .limit(100)
                            .collect(LoadCsvFromDisk.dataCollector);

                    for (float[] floats : data) {
                        analyzeData(floats);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Behaviour createBehaviourTimeout(UUID id, float[] inputData) {
        return new WakerBehaviour(this, OPINION_TIMEOUT) {
            @Override
            protected void onWake() {
                OpinionResult result = bestOpinions.remove(id);
                if (result != null) {
                    logger.info("TIMEOUT(" + id + ") : " + result.getIsAttackList());
                    float opinionsIsAttack = result.averageIsAttack();
                    analyzeResult(id, inputData, opinionsIsAttack);
                }
            }
        };
    }
}
