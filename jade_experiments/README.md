# Instruções

Este projeto faz uso da biblioteca JADE (Java Agent DEvelopment Framework) para a criação de agentes inteligentes. Ele utiliza o Maven para gerenciar as dependências e o build do projeto.

Para executar o projeto, você deve gerar o arquivo .jar do projeto com:

```shell
mvn clean install
# No test
mvn clean install "-Dmaven.test.skip=true"
```	

Este comando irá criar a pasta `target` e o arquivo `jade_experiments-1.0-SNAPSHOT.jar` dentro dela.

Para utilizar o JADE, você deve inicializar o container principal com o comando estando na pasta `env`:

```shell
cd env

java -cp "../target/jade_experiments-1.0-SNAPSHOT.jar;jade.jar" jade.Boot -gui
```
Após isso, você pode inicializar os agentes. O exemplo a seguir executa o agente HelloWorld:

```shell
java -cp "../target/jade_experiments-1.0-SNAPSHOT.jar;jade.jar" jade.Boot -container hello-world:com.ipb.examples.HelloWorld
```



Um fluxo de execução possível é o seguinte:


```shell	
mvn clean install

cd env

# Inicializa o container principal
java -cp "../target/jade_experiments-1.0-SNAPSHOT.jar;jade.jar" jade.Boot -gui

# Inicializa os agentes com acesso a um modelo de inteligência artificial
java -cp "../target/jade_experiments-1.0-SNAPSHOT.jar;jade.jar" jade.Boot -container "agent-000:com.ipb.agents.AgentAI2(randomforest, skip=0, limit=6000, confidence=0.90)" > logs/exec-randomforest.log

java -cp "../target/jade_experiments-1.0-SNAPSHOT.jar;jade.jar" jade.Boot -container "agent-001:com.ipb.agents.AgentAI2(MLP, skip=6000, limit=6000, confidence=0.90)" > logs/exec-MLP.log

java -cp "../target/jade_experiments-1.0-SNAPSHOT.jar;jade.jar" jade.Boot -container "agent-002:com.ipb.agents.AgentAI2(LogisticReg, skip=12000, limit=6000, confidence=0.90)" > logs/exec-LogisticReg.log

java -cp "../target/jade_experiments-1.0-SNAPSHOT.jar;jade.jar" jade.Boot -container "agent-003:com.ipb.agents.AgentAI2(KNN, skip=18000, limit=6000, confidence=0.90)" > logs/exec-KNN.log

java -cp "../target/jade_experiments-1.0-SNAPSHOT.jar;jade.jar" jade.Boot -container "agent-004:com.ipb.agents.AgentAI2(DecisionTree, skip=24000, limit=6000, confidence=0.90)" > logs/exec-DecisionTree.log

java -cp "../target/jade_experiments-1.0-SNAPSHOT.jar;jade.jar" jade.Boot -container "agent-005:com.ipb.agents.AgentAI2(SVM, skip=30000, confidence=0.90)" > logs/exec-SVM.log
# Inicializa o agente que mostra as metricas
java -cp "../target/jade_experiments-1.0-SNAPSHOT.jar;jade.jar" jade.Boot -container "metrics:com.ipb.agents.AgentMetrics" > logs/metrics.log

# Inicializa o agente que diz para iniciar a execução
java -cp "../target/jade_experiments-1.0-SNAPSHOT.jar;jade.jar" jade.Boot -container "start:com.ipb.agents.AgentSendReady"


# transforma os logs em csv
py ./analyze_metrics.py ./metrics_sample.log ./metrics_sample.csv
```

# Referencias

* [Jade - Tutorials Guides](https://jade.tilab.com/documentation/tutorials-guides/)
* [Jade - Manual Portuguese](https://jade.tilab.com/doc/tutorials/noEnglish/ManualJadePortuguese.pdf)
* [Jade - Colab Introdutório](https://colab.research.google.com/drive/1lfdNAxBCePx4PcKpwxM0t4-4XmLcPowU?usp=sharing)

