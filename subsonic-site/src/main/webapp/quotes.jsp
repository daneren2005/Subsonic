<script type="text/javascript">
    var quoteIndex = 0;

    var quotes = new Array();
    var i = 0;
    quotes[i++] = "Please don't ever stop development on this project! Subsonic 4 life!";
    quotes[i++] = "I've used many media servers and this is by far the best.";
    quotes[i++] = "It's the best streaming app I've ever seen! And I've tried them all.";
    quotes[i++] = "You release often and listen to user feedback. Awesome!";
    quotes[i++] = "Just set up subsonic - easy to install and working great. The features, the performance, everything is just excellent!";
    quotes[i++] = "I am extremely impressed with the stability of the program.";
    quotes[i++] = "I just switched from Jinzora and I'm really impressed about Subsonic. The performance is great.";
    quotes[i++] = "I just installed Subsonic and immediately forgot about all previous php-based jukeboxes (including my own...)";
    quotes[i++] = "Subsonic is beautiful in simplicity of the end user interface. I had no issues setting it up and the guide was brilliant.";
    quotes[i++] = "One word describes Subsonic: AWESOME!";
    quotes[i++] = "Every release is consistently better and I love the way you take feedback on board and act on it. Long live Subsonic!";

    var authors = new Array();
    i = 0;
    authors[i++] = "cup0spam";
    authors[i++] = "ClemsonJeeper";
    authors[i++] = "Eloquence";
    authors[i++] = "Eloquence";
    authors[i++] = "cwilliams";
    authors[i++] = "chugmonkey";
    authors[i++] = "k3tana";
    authors[i++] = "cellulit";
    authors[i++] = "labrat-radio";
    authors[i++] = "Ghostrider";
    authors[i++] = "Chug";

    function hideQuote() {
        $('#quote').animate({opacity: 0.0},{duration:1500});
        setTimeout(showQuote, 1700);
    }

    function showQuote() {
        $("#quote").html('<span>"' + quotes[quoteIndex] + '"&nbsp;&nbsp;&nbsp;&ndash;&nbsp;' + authors[quoteIndex ] + '</span>');
        quoteIndex = (quoteIndex + 1) % quotes.length;

        $('#quote').animate({opacity: 1.0},{duration:1500});
        setTimeout(hideQuote, 4000);
    }

    setTimeout(hideQuote, 4000);

</script>

<div class="sidebox" style="height:75px">

    <h2>What people say</h2>
    <div id="quote" style="font-size: 11px;">
        "Just the media server I need! I have been using Andromeda and AjaxAmp etc but Subsonic beats everything!"&nbsp;&nbsp;&nbsp;&ndash;&nbsp;Marc
    </div>
</div>
