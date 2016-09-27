/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package script.media;

import org.rosbuilding.common.ISystem;
import org.rosbuilding.common.media.CommandUtil;
import org.rosbuilding.common.media.IPlayer;
import org.rosbuilding.common.media.ISpeaker;

import com.google.common.base.Strings;
import com.rosalfred.core.ia.CommandPublisher;
import com.rosalfred.core.ia.rivescript.RiveScript;

import smarthome_media_msgs.msg.MediaAction;

/**
 *
 * @author Mickael Gaillard <mick.gaillard@gmail.com>
 * @author Erwan Lehuitouze <erwan.lehuitouze@gmail.com>
 *
 */
public class MediaBot extends CommandPublisher {

    public MediaBot(RiveScript rivescript) {
        super(rivescript);
    }

    public void selectSource(String source, String channel) {
        if (Strings.isNullOrEmpty(channel))
            channel = source;

        MediaAction media = new MediaAction(); // this.node.getTopicMessageFactory().newFromType(MediaAction._TYPE);
        media.setMethod("channel");
        media.setUri("channel://" + channel);
        media.setType("");

        this.publish(CommandUtil.toCommand(node, media));
    }

    // SYSTEM
    public void start() {
        this.publish(ISystem.OP_POWER);
    }

    public void shutdown() {
        this.publish(ISystem.OP_SHUTDOWN);
    }

    // SPEAKER
    public void mute() {
        this.publish(ISpeaker.OP_MUTE);
    }

    // PLAYER
    public void play() {
        this.publish(IPlayer.OP_PLAY);
    }

    public void pause() {
        this.publish(IPlayer.OP_PAUSE);
    }

    public void stop() {
        this.publish(IPlayer.OP_STOP);
    }

    public void forward() {
        this.publish(IPlayer.OP_RIGHT);
    }

    public void backward() {
        this.publish(IPlayer.OP_LEFT);
    }

    public void up() {
        this.publish(IPlayer.OP_UP);
    }

    public void down() {
        this.publish(IPlayer.OP_DOWN);
    }

    public void select() {
        this.publish(IPlayer.OP_SELECT);
    }
}
