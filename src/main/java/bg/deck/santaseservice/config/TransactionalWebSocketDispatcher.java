package bg.deck.santaseservice.config;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class TransactionalWebSocketDispatcher {

    public void send(Runnable sendAction) {

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            sendAction.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sendAction.run();
                    }
                }
        );
    }
}

