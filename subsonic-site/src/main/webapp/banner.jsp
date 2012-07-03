<script type="text/javascript" language="javascript">

    var currentSlide = 0;
    var numberSlides = 0;

    function slideTo(slideNumber, manual) {
        currentSlide = slideNumber;
        if (currentSlide > numberSlides) {
            currentSlide = 0;
        } else if (currentSlide < 0) {
            currentSlide = numberSlides;
        }
        var slidePosition = currentSlide * 900;

        var dur = (currentSlide == 0 || manual) ? 0 : 1000;
        $('#bannercontent').animate({left: '-'+slidePosition+'px'},{duration:dur});
        if (!manual) {
            setTimeout(function() {slideTo(currentSlide + 1, false);}, 10000);
        }
    }

    function prevSlide() {
        slideTo(currentSlide - 1, true);
    }

    function nextSlide() {
        slideTo(currentSlide + 1, true);
    }

    $(document).ready(function() {
        numberSlides = $('#bannercontent div.slide').length - 1;
        slideTo(0, false);
    });

</script>

<hr/>
<div id="banner-full">

    <a id="slide-prev" href="javascript:prevSlide();"></a>
    <a id="slide-next" href="javascript:nextSlide();"></a>

    <div id="bannercontent">
        <div class="slide1 slide">
            <div class="slidecontent">
                <img src="inc/img/banner/banner-01.jpg" alt="" class="screenshot"/>

                <div class="title">
                    <div class="large">The soundtrack of your life</div>
                    <div class="small">Non-stop music and video streaming.</div>
                </div>
            </div>
        </div>
        <div class="slide2 slide">
            <div class="slidecontent">
                <img src="inc/img/banner/apps.png" alt="" class="screenshot"/>

                <div class="title">
                    <div class="large">Don't leave home without it</div>
                    <div class="small"><a href="apps.jsp">Apps</a> available for Android, iPhone and Windows&nbsp;Phone&nbsp;7.</div>
                </div>
            </div>
        </div>

        <div class="slide3 slide">
            <div class="slidecontent">
                <img src="inc/img/banner/screenshot.png" alt="" class="screenshot"/>

                <div class="title">
                    <div class="large">The most complete personal streaming system</div>
                    <div class="small">Subsonic comes packed with features.</div>
                </div>
                <div class="text">
                    Podcast receiver, jukebox mode, on-the-fly downsampling and conversion,
                    multiple frontends, highly configurable, full support for tags, lyrics and album art, open API
                    and <a href="features.jsp">much more</a>.
                </div>
            </div>
        </div>
        <div class="slide4 slide">
            <div class="slidecontent">
                <img src="inc/img/banner/video.png" alt="" class="screenshot"/>

                <div class="title">
                    <div class="large">New in Subsonic 4.3</div>
                    <div class="small">Stream all your movies too!</div>
                    <img src="inc/img/banner/android-video.png" alt="" style="margin-top:20px;margin-left:90px;"/>
                </div>
            </div>
        </div>
        <div class="slide5 slide">
            <div class="slidecontent">
                <img src="inc/img/banner/car.png" alt="" class="screenshot"/>

                <div class="title">
                    <div class="large">No strings attached</div>
                    <div class="small">Relax to your favourite tunes no matter where you are.</div>
                </div>
            </div>
        </div>
    </div>
</div>
<hr/>