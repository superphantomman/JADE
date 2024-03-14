package jadelab1.homework;


import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class DictionaryServiceAgent extends Agent {
    @Override
    protected void setup() {
        DFAgentDescription dfad = new DFAgentDescription();
        dfad.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("answers");
        sd.setName("ask");

        dfad.addServices(sd);

        try {
            DFService.register(this, dfad);
        } catch (FIPAException ex) {
            ex.printStackTrace();
        }

        addBehaviour(new AskCyclicBehaviour(this));

    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            ex.printStackTrace();
        }
    }

    public String makeRequest(String serviceName, String word) {
        StringBuffer response = new StringBuffer();
        try {
            URL url;
            URLConnection urlConn;
            DataOutputStream printout;
            DataInputStream input;
            url = new URL("http://dict.org/bin/Dict");
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String content = "Form=Dict1&Strategy=*&Database=" + URLEncoder.encode(serviceName) + "&Query=" + URLEncoder.encode(word) + "&submit=Submit+query";
            //forth
            printout = new DataOutputStream(urlConn.getOutputStream());
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            //back
            input = new DataInputStream(urlConn.getInputStream());
            String str;
            while (null != ((str = input.readLine()))) {
                response.append(str);
            }
            input.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        //cut what is unnecessary
        return response.substring(response.indexOf("<hr>") + 4, response.lastIndexOf("<hr>"));
    }
}

class AskCyclicBehaviour extends CyclicBehaviour {
    public AskCyclicBehaviour(DictionaryServiceAgent agent) {
        this.agent = agent;
    }

    private final DictionaryServiceAgent agent;

    @Override
    public void action() {
        MessageTemplate template = MessageTemplate.MatchOntology("ask");
        ACLMessage message = agent.receive(template);

        if (message == null) {
            block();
            return;
        }

        String content = message.getContent();
        String language = message.getLanguage();

        ACLMessage reply = message.createReply();

        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(getResponse(language, content));
        agent.send(reply);

    }

    private String getResponse(String language, String content) {
        String response;
        try {
            response = agent.makeRequest(language, content);
        } catch (NumberFormatException ex) {
            response = ex.getMessage();
        }
        return response;
    }

}