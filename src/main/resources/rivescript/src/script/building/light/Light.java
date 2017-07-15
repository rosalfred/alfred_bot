/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package script.building.light;

import com.rosalfred.core.ia.CommandPublisher;
import com.rosalfred.core.ia.rivescript.BotReply;
import com.rosalfred.core.ia.rivescript.RiveScript;

/**
*
* @author Erwan Le Huitouze <erwan.lehuitouze@gmail.com>
*
*/
public class Light extends CommandPublisher {

    public Light(RiveScript rs) {
        super(rs);
    }

    public void changeColor(int h, int s, int b) {
        this.publish("light", String.format("%d,%d,%d", h, s, b));
    }

    public BotReply on() {
        this.publish("light", String.format("%d,%d,%d", 0, 0, 100));
        return new BotReply("");
    }

    public BotReply off() {
        this.publish("light", String.format("%d,%d,%d", 0, 0, 0));
        return new BotReply("");
    }

    public BotReply down() {
        this.publish("light", String.format("%d,%d,%d", -1, -1, 0));
        return new BotReply("");
    }
}
