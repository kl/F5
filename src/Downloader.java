import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Downloader {

    protected WebClient webClient;
    protected int topicID;
    protected HtmlPage topic;
    protected HtmlPage lastPage;
    protected int currentPageNo;
    protected ArrayList<Hashtable<String, String>> currentPosts;

    public Downloader(String username, String password, int topicID) throws Exception {
        setupWebClient();
        //login(username, password);
        setTopic(topicID);
    }

    protected void setupWebClient() {
        webClient = new WebClient(BrowserVersion.FIREFOX_3_6);
        webClient.setJavaScriptEnabled(false);
        webClient.setRedirectEnabled(true);

    }

    protected void login(String username, String password) throws Exception {
        HtmlPage page = (HtmlPage)webClient.getPage("http://forums.somethingawful.com/account.php?action=loginform");

        // Login form is the second form on the login page
        HtmlForm loginForm = page.getForms().get(1);

        loginForm.getInputByName("username").setValueAttribute(username);
        loginForm.getInputByName("password").setValueAttribute(password);
        loginForm.getInputByValue("Login!").click();
    }

    public void setTopic(int id) throws Exception {
        if (id != topicID) {
            topicID = id;
            topic = getTopic(topicID);
            lastPage = getLastPage(topic);
            currentPageNo = getPageCount(lastPage);
            currentPosts = null;
        }
    }

    public ArrayList<Hashtable<String, String>> getNewPosts() throws Exception {

        // If we have not yet loaded any posts, load all posts and return them.
        if (currentPosts == null) {
            currentPosts = getPosts(lastPage);
            return currentPosts;
        }

        lastPage = (HtmlPage)lastPage.refresh();                            // update the last page.
        int rPageNo = getPageCount(lastPage);                               // get updated page count number.
        ArrayList<Hashtable<String, String>> rPosts = getPosts(lastPage);   // get updated posts from last page.

        // A buffer that stores all new posts.
        ArrayList<Hashtable<String, String>> postBuffer = new ArrayList<Hashtable<String, String>>();

        if (rPageNo != currentPageNo) {     // if there is a new topic page.

            if (rPosts.size() != currentPosts.size()) {     // if unread posts on current page.
                List<Hashtable<String, String>> newPosts = rPosts.subList(currentPosts.size(), rPosts.size());
                postBuffer.addAll(new ArrayList<Hashtable<String, String>>(newPosts));
            }

            // Load the new last page and add it's posts to the buffer.
            lastPage = getLastPage(lastPage);
            ArrayList<Hashtable<String, String>> newPagePosts = getPosts(lastPage);
            postBuffer.addAll(newPagePosts);
            currentPosts = newPagePosts;
            currentPageNo = rPageNo;


        } else {                            // if there is no new page.

            if (rPosts.size() != currentPosts.size()) {     // if unread post on the current page.
                List<Hashtable<String, String>> newPosts = rPosts.subList(currentPosts.size(), rPosts.size());
                postBuffer.addAll(new ArrayList<Hashtable<String, String>>(newPosts));
                currentPosts = rPosts;
            } else {                        // if no new posts at all.
                postBuffer = null;
            }
        }
        return postBuffer;  // return the new posts or null.
    }

    protected HtmlPage getLastPage(HtmlPage topic) throws Exception {
        int pageCount = getPageCount(topic);

        if (pageCount > 1) {
            String lastPageURL = topic.getUrl().toString() + "&pagenumber=" + pageCount;
            return webClient.getPage(lastPageURL);
        } else {
            return topic;
        }
    }

    protected int getPageCount(HtmlPage topic) {
        HtmlElement pagesDiv = topic.getFirstByXPath("//div[@class='pages top']"); // get pages # div
        String pagesText = pagesDiv.asText();

        Pattern pagesPattern = Pattern.compile("Pages\\s+\\((\\d+)\\)");    // extract pages number
        Matcher pagesMatcher = pagesPattern.matcher(pagesText);

        if (pagesMatcher.find())                                          // if match return last page
            return Integer.parseInt(pagesMatcher.group(1));
        else                                                            // else topic has only 1 page
            return 1;
    }

    protected HtmlPage getTopic(int id) throws Exception {
        return webClient.getPage("http://forums.somethingawful.com/showthread.php?threadid=" + id);
    }

    protected ArrayList<Hashtable<String, String>> getPosts(HtmlPage page) {

        ArrayList<Hashtable<String, String>> posts = new ArrayList<Hashtable<String, String>>();

        ArrayList<HtmlElement> postTables = (ArrayList<HtmlElement>)page.getByXPath("//table[@class='post']");

        for (HtmlElement table : postTables) {

            // Note: The <tbody> tag seems to be added by htmlunit. It's not there in Opera for example.
            String username  = ((HtmlElement)table.getFirstByXPath("tbody/tr/td/dl/dt")).asText();
            String postText  = ((HtmlElement)table.getFirstByXPath("tbody/tr/td[@class='postbody']")).asXml();
            String timestamp = ((HtmlElement)table.getFirstByXPath("tbody/tr/td[@class='postdate']")).asText();

            postText  = cleanPostText(postText);
            timestamp = cleanTimestamp(timestamp);

            Hashtable<String, String> postHash = new Hashtable<String, String>();
            postHash.put("username", username);
            postHash.put("post", postText);
            postHash.put("timestamp", timestamp);

            posts.add(postHash);
        }
        return posts;
    }

    //
    // Remove the first tabs and <td> tag and the google ad stuff from the post.
    // We want a div block around the post because otherwise word wrap does not work correctly.
    //
    protected String cleanPostText(String post) {

        String result = post.replaceFirst("\\t+", "")
                            .replaceFirst("(?s)<td.+?ad_section_start\\s+-->", "")
                            .replaceFirst("(?s)<!--\\s+google_ad_section.+", "");

        return "<div>" + result + "</div>";
    }

    //
    // Remove leading characters #, ? and whitespace and then replace duplicated whitespace with " ".
    //
    protected String cleanTimestamp(String timestamp) {
        return timestamp.replaceFirst("[#?\\s]+", "")
                .replaceAll("\\s+", " ");
    }

}
