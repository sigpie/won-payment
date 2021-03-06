package won.payment.paypal.bot.action.modification;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.event.analyze.ConversationAnalyzationCommandEvent;
import won.payment.paypal.bot.event.modification.MessageRetractedEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentContext;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.agreement.ConversationMessage;
import won.protocol.agreement.ConversationMessagesReader;
import won.protocol.model.Connection;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONPAY;

/**
 * When the user retracts a message from themselves, check if the message had an
 * influence on the conversation. If yes, throws an analyze conversation event.
 * 
 * @author schokobaer
 */
public class MessageRetractedAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public MessageRetractedAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (ctx.getBotContextWrapper() instanceof PaypalBotContextWrapper && event instanceof MessageRetractedEvent) {
            logger.info("Message retracted");
            MessageRetractedEvent retractedEvent = (MessageRetractedEvent) event;
            Connection con = ((BaseAtomAndConnectionSpecificEvent) event).getCon();
            PaymentContext payCtx = ((PaypalBotContextWrapper) ctx.getBotContextWrapper())
                    .getPaymentContext(con.getAtomURI());
            if (payCtx.getStatus() != PaymentStatus.BUILDING) {
                return;
            }
            if (retractPayment(retractedEvent)) {
                ctx.getEventBus().publish(new ConversationAnalyzationCommandEvent(con));
            }
        }
    }

    /**
     * Finds out if the retracted message has an important triple for the payment.
     * If yes it retracts the current payment summary and the proposal for it (if
     * already one exists) and returns true. If no triple is found it returns false.
     * If no proposal is available yet it also returns true.
     * 
     * @param event
     * @return
     */
    private boolean retractPayment(MessageRetractedEvent event) {
        // Check if the retracted message has content of the current proposed payment ->
        // Retract Summary and Proposal
        AgreementProtocolState agreementProtocolState = AgreementProtocolState.of(event.getConnectionURI(),
                getEventListenerContext().getLinkedDataSource());
        URI lastProposalUri = agreementProtocolState.getLatestPendingProposal();
        Model lastProposal = lastProposalUri != null ? agreementProtocolState.getPendingProposal(lastProposalUri)
                : null;
        if (lastProposal == null) {
            return true;
        }
        Model retMsg = getRetractedContent(event);
        MutableBoolean containsPaymentMsg = new MutableBoolean(false);
        lastProposal.listStatements().forEachRemaining(pStmt -> {
            boolean fits = retMsg.listStatements(pStmt.getSubject(), pStmt.getPredicate(), pStmt.getObject()).hasNext();
            if (fits) {
                containsPaymentMsg.setValue(true);
            }
        });
        if (!containsPaymentMsg.booleanValue()) {
            return false;
        }
        // Find out payment summary URI
        StringBuilder paymentSummaryUriBuilder = new StringBuilder();
        lastProposal.listStatements(null, RDF.type, WONPAY.PAYMENT_SUMMARY).forEachRemaining(stmt -> {
            paymentSummaryUriBuilder.append(stmt.getSubject().getURI());
        });
        try {
            Model retractResponse = WonRdfUtils.MessageUtils.retractsMessage(lastProposalUri,
                    new URI(paymentSummaryUriBuilder.toString()));
            getEventListenerContext().getEventBus()
                    .publish(new ConnectionMessageCommandEvent(event.getCon(), retractResponse));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return true;
    }

    private Model getRetractedContent(MessageRetractedEvent event) {
        Dataset conversationDataset = WonLinkedDataUtils.getConversationAndAtomsDataset(event.getConnectionURI(),
                getEventListenerContext().getLinkedDataSource());
        conversationDataset.begin(ReadWrite.READ);
        Map<URI, ConversationMessage> conversationMessages = ConversationMessagesReader
                .readConversationMessages(conversationDataset);
        if (!conversationMessages.containsKey(event.getRetractedMessageUri())) {
            logger.warn("Retracted URI not in conversation dataset");
            return null;
        }
        ConversationMessage msg = conversationMessages.get(event.getRetractedMessageUri());
        final Model retractedContent = ModelFactory.createDefaultModel();
        msg.getContentGraphs().forEach(uri -> {
            Model graph = conversationDataset.getNamedModel(uri.toString());
            if (graph != null) {
                retractedContent.add(RdfUtils.cloneModel(graph));
            }
        });
        conversationDataset.end();
        return retractedContent;
    }
}
