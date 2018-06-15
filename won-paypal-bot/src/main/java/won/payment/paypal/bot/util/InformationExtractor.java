package won.payment.paypal.bot.util;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;

import won.utils.goals.GoalInstantiationResult;

public class InformationExtractor {

	private static final String paymentDetailsQuery;
	
    private static final String AMOUNT = "amount";
    private static final String CURRENCY = "currency";
    private static final String RECEIVER = "receiver";
    private static final String SECRET = "secret";
    private static final String COUNTERPART = "counterpart";
    private static final String FEEPAYER = "feepayer";
    private static final String TAX = "tax";
    private static final String INVOICEID = "invoiceid";
    private static final String INVOICEDETAILS = "invoicedetails";
    private static final String EXPIRATIONTIME = "expirationtime";
    
    
    
    static {
    	paymentDetailsQuery = ResourceManager.getResourceAsString("/temp/paymentDetails.rq");
    }
    
    public static Double getAmount(GoalInstantiationResult payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload.getInstanceModel());

            if (solution != null) {
                return solution.getLiteral(AMOUNT).getDouble();
            }
        }
        return null;
    }
    
    public static String getCurrency(GoalInstantiationResult payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload.getInstanceModel());

            if (solution != null) {
                return solution.getLiteral(CURRENCY).getString();
            }
        }
        return null;
    }
    
    public static String getReceiver(GoalInstantiationResult payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload.getInstanceModel());

            if (solution != null) {
                return solution.getLiteral(RECEIVER).getString();
            }
        }
        return null;
    }
    
    public static String getSecret(GoalInstantiationResult payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload.getInstanceModel());

            if (solution != null) {
                return solution.getLiteral(SECRET).getString();
            }
        }
        return null;
    }
    
    public static String getCounterpart(GoalInstantiationResult payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload.getInstanceModel());

            if (solution != null) {
                return solution.getResource(COUNTERPART).getURI();
            }
        }
        return null;
    }
    
    // Optional
    public static String getFeePayer(GoalInstantiationResult payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload.getInstanceModel());

            if (solution != null && solution.getLiteral(FEEPAYER) != null) {
                return solution.getLiteral(FEEPAYER).getString();
            }
        }
        return null;
    }
    
    public static Double getTax(GoalInstantiationResult payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload.getInstanceModel());

            if (solution != null && solution.getLiteral(TAX) != null) {
                return solution.getLiteral(TAX).getDouble();
            }
        }
        return null;
    }
    
    public static String getInvoiceId(GoalInstantiationResult payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload.getInstanceModel());

            if (solution != null && solution.getLiteral(INVOICEID) != null) {
                return solution.getLiteral(INVOICEID).getString();
            }
        }
        return null;
    }
    
    public static String getInvoiceDetails(GoalInstantiationResult payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload.getInstanceModel());

            if (solution != null && solution.getLiteral(INVOICEDETAILS) != null) {
                return solution.getLiteral(INVOICEDETAILS).getString();
            }
        }
        return null;
    }
    
    public static String getExpirationTime(GoalInstantiationResult payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload.getInstanceModel());

            if (solution != null && solution.getLiteral(EXPIRATIONTIME) != null) {
                return solution.getLiteral(EXPIRATIONTIME).getString();
            }
        }
        return null;
    }
    
    
    
    
    
    
    private static QuerySolution executeQuery(String queryString, Model payload) {
        Query query = QueryFactory.create(queryString);
        try(QueryExecution qexec = QueryExecutionFactory.create(query, payload)){
            ResultSet resultSet = qexec.execSelect();
            if (resultSet.hasNext()){
                QuerySolution solution = resultSet.nextSolution();
                return solution;
            }
        }
        return null;
    }
}
