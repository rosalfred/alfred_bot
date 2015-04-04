/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package script.media.youtube;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rosbuilding.common.media.IPlayer;

import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.VideoFeed;
import com.google.gdata.util.ServiceException;
import com.rosalfred.core.ia.IaNode;
import com.rosalfred.core.ia.RosRiveScript;
import com.rosalfred.core.ia.RsContext;
import com.rosalfred.core.ia.rivescript.BotReply;

import script.media.xbmc.Xbmc;

/**
 *
 * @author Mickael Gaillard <mick.gaillard@gmail.com>
 * @author Erwan Lehuitouze <erwan.lehuitouze@gmail.com>
 *
 */
public class Youtube {

    private final RosRiveScript rivescript;

    public Youtube(RosRiveScript rivescript) {
        this.rivescript = rivescript;
    }

    /**
     * Search videos on youtube from user request.
     * @return Number of finded videos
     */
    public BotReply search() {
        RsContext rsUtils = new RsContext(this.rivescript);

        String search = this.getSearch();
        int index = this.rivescript.getUtils().getIndexItemPage();
        URL url = this.getSearchUrl(search, index + 1, rsUtils.getStep());

        YouTubeService service = new YouTubeService(IaNode.botname);
        VideoFeed videos = null;

        try {
            videos = service.getFeed(url, VideoFeed.class);
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<VideoEntry> entries = videos.getEntries();
        int total = entries.size();
        String titles = "";

        for (VideoEntry entry : entries) {
            titles += RsContext.normalize(entry.getTitle().getPlainText()) + " ";
        }

        rsUtils.setUserParam(RsContext.COUNT, String.valueOf(total));
        rsUtils.setUserParam(RsContext.FINDING, titles);

        return new BotReply(String.valueOf(total));
    }

    /**
     * Get url from user answer and publish it to library.
     * @return Name of launched video
     */
    public BotReply launch() {
        RsContext rsUtils = new RsContext(this.rivescript);
        String result = "";

        String search = this.getSearch();
        String item = RsContext.normalize(rsUtils.getUserParam(RsContext.LAUNCH));
        int index = rsUtils.getIndexItemPage() + 1; //Because Youtube start a 1
        URL url = this.getSearchUrl(search, index + 1, rsUtils.getStep());

        YouTubeService service = new YouTubeService("alfred");
        VideoFeed videos = null;

        try {
            videos = service.getFeed(url, VideoFeed.class);
        } catch (ServiceException | IOException e) {

        }

        List<VideoEntry> entries = videos.getEntries();
        String src = null;

        for (VideoEntry entry : entries) {
            String title = RsContext.normalize(
                    entry.getTitle().getPlainText().toLowerCase());

            if (title.contains(item.toLowerCase())) {
                src = ((MediaContent)entry.getContent()).getUri();
                result = RsContext.normalize(entry.getTitle().getPlainText());
                break;
            }
        }

        if (src != null && !src.equals("")) {
            new Xbmc(this.rivescript).open(
                    IPlayer.URI_MEDIA_YOUTUBE + this.getVideoId(src));
        }

        return new BotReply(result);
    }

    /**
     * Get value to search from rivescript user params.
     * @return Value to search
     */
    private String getSearch() {
        String result = "";

        try {
            result = URLEncoder.encode(
                    this.rivescript.getUtils().getUserParam(RsContext.FIND),
                    "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        return result;
    }

    /**
     * Get Youtube API Search url.
     * @param search Video to search
     * @param startIndex Start index (first index is 1)
     * @param maxResults Maximum returned results
     * @return Search url
     */
    private URL getSearchUrl(String search, int startIndex, int maxResults) {
        URL result = null;

        String params = String.format("q=%s&start-index=%s&max-results=%s&v=2",
                search, startIndex, maxResults);

        try {
            result = new URL(String.format(
                    "http://gdata.youtube.com/feeds/api/videos?%s",
                    params));
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Get Video id from url (youtube).
     * @param youtubeUrl
     * @return
     */
    private String getVideoId(String youtubeUrl) {
        /*
         * Examples: -
         * http://www.youtube.com/watch?v=k0BWlvnBmIE (General URL)
         * http://youtu.be/k0BWlvnBmIE (Share URL)
         * http://www.youtube.com/watch?v=UWb5Qc-fBvk&list=FLzH5IF4Lwgv-DM3CupM3Zog&index=2 (Playlist URL)
         */

        String result = null;

        String pattern = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(youtubeUrl);
        int youtu = youtubeUrl.indexOf("youtu");

        if (m.matches() && youtu != -1) {
            int ytu = youtubeUrl.indexOf("http://youtu.be/");

            if (ytu != -1) {
                String[] split = youtubeUrl.split(".be/");
                result = split[1];
            } else {
                URL youtube = null;

                try {
                    youtube = new URL(youtubeUrl);
                } catch (MalformedURLException e) {
                    this.rivescript.getNode().getLog().error(
                            "Bad Youtube url + " + youtubeUrl);
                }

                if (youtube != null) {
                    if (youtube.getPath().startsWith("/v/")) {
                        result = youtube.getPath().substring(3);
                    } else {
                        String[] split = youtube.getQuery().split("=");
                        int query = split[1].indexOf("&");

                        if (query != -1) {
                            String[] nSplit = split[1].split("&");
                            result = nSplit[0];
                        } else {
                            result = split[1];
                        }
                    }
                }
            }
        }

        return result;
    }
}
