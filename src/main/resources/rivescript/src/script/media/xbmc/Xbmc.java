/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package script.media.xbmc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.ros.exception.RemoteException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.rosbuilding.common.ISystem;
import org.rosbuilding.common.media.CommandUtil;
import org.rosbuilding.common.media.IPlayer;
import org.rosbuilding.common.media.ISpeaker;
import org.rosmultimedia.player.media.model.Media;
import org.rosmultimedia.player.media.model.Movie;
import org.rosmultimedia.player.media.model.Song;
import org.rosmultimedia.player.media.model.Tvshow;

import com.rosalfred.core.ia.CommandPublisher;
import com.rosalfred.core.ia.RsContext;
import com.rosalfred.core.ia.rivescript.BotReply;
import com.rosalfred.core.ia.rivescript.RiveScript;

import media_msgs.MediaGetItems;
import media_msgs.MediaGetItemsRequest;
import media_msgs.MediaGetItemsResponse;
import media_msgs.MediaItem;
import media_msgs.MediaType;

import building_msgs.Command;

/**
 *
 * @author Erwan Lehuitouze <erwan.lehuitouze@gmail.com>
 *
 */
public class Xbmc extends CommandPublisher {

    private final static String nodePath = "/home/salon/xbmc/";
    private static ServiceClient<MediaGetItemsRequest, MediaGetItemsResponse> service;

    public Xbmc(RiveScript rivescript) {
        super(rivescript);
    }

    @Override
    protected void initialize() {
        super.initialize();

        if (this.node != null) {
            try {
                service = this.node.newServiceClient(nodePath + "get_items", MediaGetItems._TYPE);
            } catch (ServiceNotFoundException e) {
                this.node.getLog().error("Service Xbmc get_items not found !");
            }
        }
    }

    public void startLibrary() {
        // new Onkyo(this.rivescript).startReceiver();
        this.publish(ISystem.OP_POWER);
    }

    public void shutdownLibrary() {
        // new Onkyo(this.rivescript).shutdownReceiver();
        this.publish(ISystem.OP_SHUTDOWN);
    }

    public void open(String uri) {
        this.open(uri, null);
    }

    public void open(String uri, String type) {
        Command message = null;
        try {
            message = CommandUtil.toCommand(this.node, IPlayer.OP_OPEN, new URI(uri), type);
        } catch (URISyntaxException e) {
            this.node.getLog().error("Data no a uri !!", e);
        }

        this.publish(message);
    }

    public void mute() {
        this.publish(ISpeaker.OP_MUTE);
    }

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

    public void playto() {
        // this.publish("power");
    }

    public BotReply searchMediaMovie() {
        String search = this.getUserParam(RsContext.FIND);
        String type = this.getUserParam(RsContext.FIND2);
        if (type == null) {
            type = "";
        }

        List<MediaItem> items;

        if (service != null) {
            Movie media = new Movie();
            MediaGetItemsRequest request = service.newMessage();
            request.getItem().getMediatype().setValue(MediaType.VIDEO_MOVIE);

            switch (type) {
            case "cast":
                media.setCast(new ArrayList<String>());
                media.getCast().add(search);
                break;
            case "year":
                media.setYear(Integer.parseInt(search));
                break;
            case "genre":
                media.setGenre(new ArrayList<String>());
                media.getGenre().add(search);
                break;
            default:
                media.setTitle(search);
                break;
            }

            request.getItem().setData(media.toJson());
            items = this.getMediaItems(request);
        } else {
            items = new ArrayList<MediaItem>();
        }

        this.setUserParam(RsContext.COUNT, String.valueOf(items.size()));

        String titles = "";
        List<Media> medias = null;

        if (items.size() == 1) {
            MediaItem item = items.get(0);
            Media media = Movie.fromJson(item.getData());
            titles = media.getTitle();
            this.setUserParam(RsContext.LAUNCH, media.getTitle());
        } else if (items.size() > 1) {
            medias = new ArrayList<Media>();
            Media media = null;

            for (MediaItem item : items) {
                media = Movie.fromJson(item.getData());
                String title = RsContext.normalize(media.getTitle());
                titles += title + ".  ";
                medias.add(media);
            }
        }

        this.setUserParam(RsContext.FINDING, titles);

        BotReply reply = new BotReply(String.valueOf(items.size()));

        if (medias != null) {
            for (Media media : medias) {
                reply.getIntents().add(media.toJson());
            }
        }

        return reply;
    }

    public BotReply searchMediaTvshows() {
        String show = this.getUserParam(RsContext.FIND);
        String season = this.getUserParam(RsContext.FIND2);
        String episode = this.getUserParam(RsContext.FIND3);

        MediaGetItemsRequest request = service.newMessage();
        MediaItem reqItem = request.getItem();
        reqItem.getMediatype().setValue(MediaType.VIDEO_TVSHOW_EPISODE);

        Tvshow media = new Tvshow();
        media.setShowtitle(show);
        media.setSeason(Integer.valueOf(season));
        media.setEpisode(Integer.valueOf(episode));
        media.setPlaycount(-1); // force playcount to -1 to ignore it

        reqItem.setData(media.toJson());

        List<MediaItem> items = this.getMediaItems(request);
        this.setUserParam(RsContext.COUNT, String.valueOf(items.size()));

        String titles = "";
        if (items.size() == 1) {
            MediaItem item = items.get(0);
            media = Tvshow.fromJson(item.getData());
            titles = media.getShowtitle();
            this.setUserParam(RsContext.LAUNCH, media.getShowtitle());
        } else if (items.size() > 1) {
            for (MediaItem item : items) {
                media = Tvshow.fromJson(item.getData());
                String title = RsContext.normalize(media.getShowtitle());
                titles += title + ".  ";
            }
        }

        this.setUserParam(RsContext.FINDING, titles);

        return new BotReply(String.valueOf(items.size()));
    }

    public BotReply searchMediaAudioAlbum() {
        String search = this.getUserParam(RsContext.FIND);
        String type = this.getUserParam(RsContext.FIND2);
        if (type == null) {
            type = "";
        }

        MediaGetItemsRequest request = service.newMessage();
        request.getItem().getMediatype().setValue(MediaType.AUDIO_ALBUM);
        Song media = new Song();

        switch (type) {
            default:
                media.setAlbum(search);
                break;
        }

        List<MediaItem> items = this.getMediaItems(request);

        this.setUserParam(RsContext.COUNT, String.valueOf(items.size()));

        String titles = "";
        if (items.size() == 1) {
            MediaItem item = items.get(0);
            media = Song.fromJson(item.getData());
            titles = media.getTitle();
            this.setUserParam(RsContext.LAUNCH, media.getAlbum());
        } else if (items.size() > 1) {
            for (MediaItem item : items) {
                media = Song.fromJson(item.getData());
                String title = RsContext.normalize(media.getAlbum());
                titles += title + ".  ";
            }
        }

        this.setUserParam(RsContext.FINDING, titles);

        return new BotReply(String.valueOf(items.size()));
    }

    public BotReply launchMediaMovie() {
        String search = this.getUserParam(RsContext.FIND);
        String type = this.getUserParam(RsContext.FIND2);
        if (type == null) {
            type = "";
        }

        String launch = this.getUserParam(RsContext.LAUNCH);
        launch = RsContext.normalize(launch).toLowerCase();
        String title = null;

        if (service != null) {
            MediaGetItemsRequest request = service.newMessage();
            request.getItem().getMediatype().setValue(MediaType.VIDEO_MOVIE);
            Movie media = new Movie();

            switch (type) {
                case "cast":
                    media.setCast(new ArrayList<String>());
                    media.getCast().add(search);
                    break;
                case "year":
                    media.setYear(Integer.parseInt(search));
                    break;
                case "genre":
                    media.setGenre(new ArrayList<String>());
                    media.getGenre().add(search);
                    break;
                default:
                    media.setTitle(search);
                    break;
            }

            request.getItem().setData(media.toJson());
            List<MediaItem> items = this.getMediaItems(request);

            if (items.size() > 0) {
                for (MediaItem item : items) {
                    media = Movie.fromJson(item.getData());
                    title = RsContext.normalize(media.getTitle()).toLowerCase();
                    if (title.contains(launch)) {
                        this.open(
                                "media://" + String.valueOf(item.getMediaid()),
                                "MediaMovie");
                        break;
                    }
                }
            }
        }

        return new BotReply(title);
    }

    public BotReply launchMediaTvshow() {
        String launch = this.getUserParam(RsContext.LAUNCH);
        String show = this.getUserParam(RsContext.FIND);
        String season = this.getUserParam(RsContext.FIND2);
        String episode = this.getUserParam(RsContext.FIND3);
        // String resume = this.getUserParam(RsContext.RESUME);
        launch = RsContext.normalize(launch).toLowerCase();

        MediaGetItemsRequest request = service.newMessage();
        request.getItem().getMediatype().setValue(MediaType.VIDEO_TVSHOW_EPISODE);

        Tvshow media = new Tvshow();
        media.setShowtitle(show);
        media.setSeason(Integer.valueOf(season));
        media.setEpisode(Integer.valueOf(episode));
        media.setPlaycount(-1); // force playcount to -1 to ignore it

        List<MediaItem> items = this.getMediaItems(request);
        String title = null;

        if (items.size() > 0) {
            for (MediaItem item : items) {
                media = Tvshow.fromJson(item.getData());
                title = RsContext.normalize(media.getShowtitle()).toLowerCase();
                if (title.contains(launch)) {
                    this.open(
                            "media://" + String.valueOf(item.getMediaid()),
                            "MediaMovie");
                    break;
                }
            }
        }

        return new BotReply(title);
    }

    public BotReply launchMediaLastTvshow() {
        String show = this.getUserParam(RsContext.FIND);
        // String resume =
        // this.rivescript.getUtils().getUserParam(RsContext.RESUME);
        show = RsContext.normalize(show).toLowerCase();

        MediaGetItemsRequest request = service.newMessage();
        request.getItem().getMediatype().setValue(MediaType.VIDEO_TVSHOW_EPISODE);

        Tvshow media = new Tvshow();
        media.setShowtitle(show);
        media.setPlaycount(-1);
        request.setLimitEnd(1);

        List<MediaItem> items = this.getMediaItems(request);
        String title = null;
        int result = 0;

        if (items.size() == 0) {
            // TODO we need to call info for a show not for specific episode
            // service = rospy.ServiceProxy(self.nodePath + 'get_items',
            // MediaGetItems)
            // msg = MediaItem()
            // msg.mediatype = "episode"
            // msg.tvshow.showtitle = show
            // limits = MediaLimits()
            // limits.end = 1
            // result = service(msg, limits)
            // tvshows = result.items
            // resultats_library_count = len( tvshows )
            result = 2; // 2 = no tvshow
        } else if (items.size() > 0) {
            for (MediaItem item : items) {
                media = Tvshow.fromJson(item.getData());
                title = RsContext.normalize(media.getTitle()).toLowerCase();
                if (title.contains(show)) {
                    this.open(
                            "media://" + String.valueOf(item.getMediaid()),
                            "MediaMovie");
                    result = 1;
                    break;
                }
            }
        }

        return new BotReply(String.valueOf(result));
    }

    public BotReply launchMediaAudioAlbum() {
        String find = this.getUserParam(RsContext.FIND);
        String launch = this.getUserParam(RsContext.LAUNCH);
        launch = RsContext.normalize(launch).toLowerCase();

        MediaGetItemsRequest request = service.newMessage();
        request.getItem().getMediatype().setValue(MediaType.AUDIO_ALBUM);
        Song media = new Song();
        media.setAlbum(find);

        List<MediaItem> items = this.getMediaItems(request);
        String title = null;

        if (items.size() > 0) {
            for (MediaItem item : items) {
                media = Song.fromJson(item.getData());
                title = RsContext.normalize(media.getAlbum()).toLowerCase();
                if (title.contains(launch)) {
                    this.open(
                            "media://" + String.valueOf(item.getMediaid()),
                            "MediaAudioAlbum");
                    break;
                }
            }
        }

        return new BotReply(title);
    }

    private List<MediaItem> getMediaItems(MediaGetItemsRequest request) {
        final List<MediaItem> result = new ArrayList<MediaItem>();

        final Semaphore semaphore = new Semaphore(1);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        service.call(request, new ServiceResponseListener<MediaGetItemsResponse>() {

            @Override
            public void onSuccess(MediaGetItemsResponse response) {
                result.addAll(response.getItems());
                semaphore.release();
            }

            @Override
            public void onFailure(RemoteException arg0) {
                semaphore.release();
            }
        });

        try {
            semaphore.tryAcquire(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;
    }
}
