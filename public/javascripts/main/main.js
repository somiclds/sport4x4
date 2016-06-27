(function($) {
    $(function() { //on DOM ready

        jQuery(window).load(function() {
            // Fixed navbar
            var navbar = $('#nav-container'),
                distance = $("#header").height();
            $window = $(window);

            $(window).resize(function () {
                distance = $("#header").height();
            })
            $(window).scroll(function() {
                if ($window.scrollTop() >= distance) {
                    navbar.removeClass('navbar-fixed-top').addClass('navbar-fixed-top');
                    $("#content-container").css("padding-top", $("#nav-container").height());
                } else {
                    navbar.removeClass('navbar-fixed-top');
                    $("#content-container").css("padding-top", "0px");
                }
            });
            $("#scroller").simplyScroll();

            // Wrap images with link to be able to view them using lightbox
            $(".post-block .post-body img").each(function () {
                var block_id = $(this).closest(".post-block").attr("postid");
                $(this).wrap("<a href='" + this.src + "' data-lightbox='" +block_id +"'/>");
            });
        });
    });
})(jQuery);
