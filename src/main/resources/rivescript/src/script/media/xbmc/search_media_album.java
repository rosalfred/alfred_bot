/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package script.media.xbmc;

import com.rosalfred.core.ia.rivescript.BotReply;
import com.rosalfred.core.ia.rivescript.RiveScript;
import com.rosalfred.core.ia.rivescript.lang.java.RunRiver;

/**
 *
 * @author Erwan Lehuitouze <erwan.lehuitouze@gmail.com>
 *
 */
public class search_media_album implements RunRiver {

    @Override
    public BotReply invoke(RiveScript rs, String user, String[] args) {
        return new Xbmc(rs).searchMediaAudioAlbum();
    }
}
