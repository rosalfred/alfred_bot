/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package script.media;

import com.rosalfred.core.ia.rivescript.BotReply;
import com.rosalfred.core.ia.rivescript.RiveScript;
import com.rosalfred.core.ia.rivescript.lang.java.RunRiver;

/**
 *
 * @author Mickael Gaillard <mick.gaillard@gmail.com>
 * @author Erwan Lehuitouze <erwan.lehuitouze@gmail.com>
 *
 */
public class select_source implements RunRiver {

    @Override
    public BotReply invoke(RiveScript rs, String user, String[] args) {
        String command = args[0];
        String channel = null;

        if (args.length > 1) {
            channel = args[1];
        }
        new MediaBot(rs).selectSource(command, channel);

        return new BotReply("");
    }
}
