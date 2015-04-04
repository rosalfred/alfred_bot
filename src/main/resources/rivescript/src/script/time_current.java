/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package script;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.rosalfred.core.ia.rivescript.BotReply;
import com.rosalfred.core.ia.rivescript.RiveScript;
import com.rosalfred.core.ia.rivescript.lang.java.RunRiver;
import com.google.common.base.Joiner;

/**
 *
 * @author Mickael Gaillard <mick.gaillard@gmail.com>
 * @author Erwan Le Huitouze <erwan.lehuitouze@gmail.com>
 *
 */
public class time_current implements RunRiver {

    @Override
    public BotReply invoke(RiveScript rs, String user, String[] args) {
        DateTimeFormatter formatter;

        if (args.length > 0) {
            formatter = DateTimeFormat.forPattern(Joiner.on(" ").join(args));
        } else {
            formatter = DateTimeFormat.shortTime();
        }

        return new BotReply(formatter.print(new DateTime()));
    }

}
