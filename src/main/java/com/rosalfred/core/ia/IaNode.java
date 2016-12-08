/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.rosalfred.core.ia;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.rivescript.ClientManager;


import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.namespace.GraphName;
import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.node.topic.SubscriptionCallback;
import org.ros2.rcljava.node.topic.Publisher;
import org.ros2.rcljava.node.topic.Subscription;
import org.rosbuilding.common.BaseSimpleNode;
import org.rosbuilding.common.media.CommandUtil;

import com.rosalfred.core.ia.rivescript.BotReply;
import com.rosalfred.core.ia.rivescript.RiveScript;
import com.rosalfred.core.ia.rivescript.lang.Echo;
import com.rosalfred.core.ia.rivescript.lang.Java;

import smarthome_comm_msgs.msg.Command;
/**
 *
 * @author Mickael Gaillard <mick.gaillard@gmail.com>
 * @author Erwan Lehuitouze <erwan.lehuitouze@gmail.com>
 *
 */
public class IaNode extends BaseSimpleNode<IaConfig> implements SubscriptionCallback<Command> {

    public static final String VAR_CONTEXT_WHERE    = "context-where";
    public static final String SUB_CMD              = "/speech";
    public static final String PUB_STATE            = "/robotsay";
    private static final String PERSITE_FILE        = "saveContext.xml";

    public static String botname = "Alfred";

    protected String path = "rivescript";

    protected Publisher<Command> publisherSay;
    protected Subscription<Command> subscriberListen;

    protected RosRiveScript bot;

//    @Override
//    public GraphName getDefaultNodeName() {
//        return GraphName.of("local_ia");
//    }

    @Override
    public void onStart(Node connectedNode) {
        super.onStart(connectedNode);

        this.path = this.getPath();

        this.initTopics();
        this.initBot();

        this.sayWelcome();
    }

    @Override
    protected IaConfig makeConfiguration() {
        return new IaConfig(this.getConnectedNode());
    }

    /**
     * On node shutdown is throw.
     */
    @Override
    public void onShutdown(Node node) {
        this.persistBotState();
        this.sayGoodbye();
        super.onShutdown(node);
    }

    protected String getPath() {
        return IaNode.class.getProtectionDomain().getCodeSource()
                .getLocation().getPath().replace("alfred_bot.jar", "") + "/res";
    }

    private void initBot() {
        this.sayReload();
        this.bot = this.getRiveScript();

        // TODO learning

        this.loadHandlers();
        this.reloadBot();
    }

    protected RosRiveScript getRiveScript() {
        return new RosRiveScript(this, false);
    }

    protected void loadHandlers() {
        // RiveScript
        this.logI("\tLoad engine...");
        this.bot.setHandler(RiveScript.TYPE_PERL, new Echo(this.bot)); // new
                                                         // com.rivescript.lang.Perl(rs,
                                                         // "./lang/rsp4j.pl"));
        this.bot.setHandler(RiveScript.TYPE_PYTHON, new Echo(this.bot));
        this.bot.setHandler(RiveScript.TYPE_JAVA, new Java(this.bot, this.path + "/src"));
    }

    private void reloadBot() {
        if (this.bot != null) {
            this.logI(String.format("\tLoad rules... in %s", this.path));
            this.bot.loadDirectory(this.path);

            this.bot.sortReplies();
            this.reloadBotState();

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Persist current context.
     *
     * @throws FileNotFoundException
     */
    public void persistBotState() {
        if (this.bot != null) {
            XMLEncoder encoder = null;
            String path = this.getFolderPersist();

            File file = new File(path + "/" + PERSITE_FILE);
            try {
                if (!file.exists() && !file.createNewFile()) {
                    this.logE("File couldn't be created...");
                }
                encoder = new XMLEncoder(new FileOutputStream(file));
                encoder.writeObject(this.bot.getUservars());
                encoder.flush();
            } catch (FileNotFoundException e) {
                this.logE(e);
            } catch (IOException e) {
                this.logE(e);
            } finally {
                if (encoder != null)
                    encoder.close();
            }
        }
    }

    /**
     * @return Folder persist path.
     */
    protected String getFolderPersist() {
        String path = System.getProperty("user.home") + "/.ros2";
        if (System.getenv().containsKey("ROS_HOME")) {
            path = System.getenv("ROS_HOME");
        }
        return path;
    }

    /**
     * Load from last persist context.
     */
    public void reloadBotState() {
        if (this.bot != null) {
            Object object = null;
            XMLDecoder decoder = null;
            ClientManager clientManager;
            String path = this.getFolderPersist();

            File file = new File(path + "/" + PERSITE_FILE);
            if (file.exists()) {
                try {
                    decoder = new XMLDecoder(new FileInputStream(file));
                    object = decoder.readObject();
                    clientManager = (ClientManager) object;
                    this.bot.setUservars(clientManager);
                } catch (Exception e) {
                    this.logI(e.getMessage());
                } finally {
                    if (decoder != null)
                        decoder.close();
                }
            }
        }
    }

    @Override
    protected void initTopics() {
        super.initTopics();
        this.logI("Start Topics (publishers/subscribers)...");

        // Create publishers
        this.publisherSay = this.connectedNode.createPublisher(
                Command.class,
                GraphName.getFullName(this.connectedNode, PUB_STATE, null));

        // Create subscribers
        this.subscriberListen = this.connectedNode.createSubscription(
                Command.class,
                GraphName.getFullName(this.connectedNode, SUB_CMD, null),
                this
        );
    }

    private Command makeSay() {
        Command command = new Command();
        command.getContext().setWho(IaNode.botname);
        command.setAction(CommandUtil.Action.SAY.getValue());
        return command;
    }

    private void sayReload() {
        Command command = this.makeSay();
        command.setSubject("Chargement de ma base de donnée...");
        this.publisherSay.publish(command);
    }

    private void sayGoodbye() {
        Command command = this.makeSay();
        command.setSubject("Arrêt du système...");
        this.publisherSay.publish(command);
    }

    private void sayWelcome() {
        Command command = this.makeSay();
        command.setSubject("Système prêt!");
        this.publisherSay.publish(command);
    }

    /**
     * Load parameters of node
     */
    @Override
    public void loadParameters() {
        super.loadParameters();
        // Prefix
//        this.prefix = String.format("/%s/", this.connectedNode
//                .getParameterTree().getString("~tf_prefix", ""));

//        if (this.prefix.equals("//")) // Hack
//            this.prefix = "/";

        // Path resources
//        this.path = this.connectedNode.getParameterTree()
//                .getString("~" + IaConfig.RES_PATH, this.path);
//        this.connectedNode.getParameterTree().set("~" + IaConfig.RES_PATH, this.path);

//        this.logI(String.format("prefix : %s", this.prefix));

        // Connect to dynamic reconfigure
//        this.reconfigServer = new Server<IaConfig>(
//                this.connectedNode, new IaConfig(this.connectedNode), this);
    }

    /**
     * On new message is throw.
     */
    @Override
    public void dispatch(final Command command) {
        final String user = command.getContext().getWho();
        final String where = command.getContext().getWhere();

        Thread thTmp = new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

        if (bot != null) {
            String speech = command.getSubject();
            logI(String.format("from %s : %s", user, speech));

            bot.setUservar(user, VAR_CONTEXT_WHERE, where);

            // TODO move now to Rive workflow
            switch (speech) {
            case "/reload":
                initBot();
                break;
            case "/save":
                persistBotState();
                break;
            case "/load":
                reloadBotState();
                break;
            case "/status":
                reloadBotState();
                break;
            default:
                if (!user.equals(IaNode.botname)) {
                    String escapeString = speech.replaceAll("-:", " "); // ignore
                    String[] res = escapeString.split("[.!?;]+\\s*");

                    BotReply responce = new BotReply("ERR: Sentence not work !!");
                    for (String sentence : res) {
                        responce = bot.reply(user, sentence);
                    }
                    command.getContext().setWho(IaNode.botname);

                    if (!Strings.isNullOrEmpty(responce.getReply())
                            && !responce.getReply().startsWith("ERR:")) {

                        command.setSubject(responce.getReply());
                        publisherSay.publish(command);

                        logI(String.format("from %s to %s : %s",
                                command.getContext().getWho(),
                                user,
                                command.getSubject()));

                        if (!responce.getIntents().isEmpty()) {
                            String content = String.format("[%s]",
                                    Joiner.on(",").join(responce.getIntents()));

                            Command commandTmp = new Command(); // this.publisherSay.newMessage();
                            commandTmp.getContext().setWhere(where);
                            commandTmp.getContext().setWho(IaNode.botname);
                            commandTmp.setAction("show");
                            commandTmp.setSubject(content);

                            publisherSay.publish(commandTmp);
                        }

                    } else {
                        if (Strings.isNullOrEmpty(responce.getReply())
                                || responce.getReply().contains("ERR: No Reply Matched")) {
                            // TODO Learn...
                        } else if (responce.getReply().startsWith("ERR:")) {
                            logE(String.format("bot error : %s", responce));
                        } else {
                            logI(String.format("bot command : %s", responce));
                        }
                    }
                }
            }
        } else {
            command.setSubject("IA loading...");
            publisherSay.publish(command);
            logI(String.format("from %s to %s : %s",
                    command.getContext().getWho(), user, command.getSubject()));
        }


            }
        });

        thTmp.start();
    }

//    @Override
//    public IaConfig onReconfigure(IaConfig config, int level) {
//        this.path = config.getString(IaConfig.RES_PATH, this.path);
//
//        // TODO DELETE : For test
//        config.setString(IaConfig.RES_PATH, this.path);
//
//        this.reloadBot();
//        return config;
//    }

    public static void main(String[] args) throws InterruptedException {
        // Initialize RCL
        RCLJava.rclJavaInit();

        // Let's create a Node
        Node node = RCLJava.createNode("/home", "ia_base");

        IaNode ia = new IaNode();
        ia.onStart(node);

        RCLJava.spin(node);

        ia.onShutdown(node);
        node.dispose();
        RCLJava.shutdown();
    }
}
