class A:
    def createBehaviourAskForOpinion(self, result):
        class OneShotBehaviourInternal(OneShotBehaviour):
            def action(self):
                ...

        return OneShotBehaviourInternal()