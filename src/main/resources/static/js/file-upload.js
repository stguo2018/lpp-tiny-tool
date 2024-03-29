$(function () {
    var md5;

//监听分块上传过程中的三个时间点
    WebUploader.Uploader.register({
        "before-send-file": "beforeSendFile",
        "before-send": "beforeSend",
        "after-send-file": "afterSendFile"
    }, {
        // 时间点1，所有分块上传前调用此函数
        beforeSendFile: function (file) {
            var deferred = WebUploader.Deferred();
            // 1.计算文件的唯一标记，用于断点续传
            (new WebUploader.Uploader()).md5File(file, 0, 50 * 1024 * 1024)
                .progress(function (percentage) {
                    $('#' + file.id).find("p.state").text("正在读取文件信息...");
                }).then(function (val) {
                md5 = val;
                $('#' + file.id).find("p.state").text("成功获取文件信息...");
                // 获取文件信息后进入下一步
                deferred.resolve();
            });
            return deferred.promise();
        },
        // 时间点2，如果有分块上传，则每个分块上传之前调用次函数
        beforeSend: function (block) {
            var defreed = WebUploader.Deferred();
            $.ajax({
                type: "GET",
                url: "./upload/chunk",
                data: {
                    // 文件唯一标记
                    "md5": md5,
                    // 当前分块下标
                    "chunk": block.chunk,
                    // 当前分块大小
                    "chunkSize": block.end - block.start
                },
                // 无缓存
                cache: false,
                // 无异步
                async: false,
                dataType: "json",
                success: function (data) {
                    if (data.message === "reject") {
                        // 分块存在，跳过
                        defreed.reject();
                    } else {
                        // 分块不存在或不完整
                        defreed.resolve();
                    }
                }
            });

            this.owner.options.formData.md5 = md5;
            defreed.resolve();
            return defreed.promise();
        },
        // 时间点3，所有分块上传成功后调用次函数
        afterSendFile: function () {
            // 如果分块上传成功，则通知后台合并分块
            $.ajax({
                type: "POST",
                url: "./upload/merge",
                data: {
                    md5: md5
                },
                success: function (response) {
                    alert("上传成功");
                    var path = "uploads/" + md5 + ".txt";
                    $("#item1").attr("src", path);
                }
            });
        }
    });

    var uploader = WebUploader.create({
        // swf文件路径
        swf: "../webuploader/Uploader.swf",
        // 文件服务接收端
        server: "./upload/upload",
        // 选择文件按钮，可选
        // 内部根据当前运行创建，可能是input元素，也可能是flash
        pick: "#picker",
        // 不压缩image，如果默认是jpeg，文件上传前会压缩一把再上传！
        resize: false,
        // 开启分片上传
        chunked: true,
        // 分片大小
        chunkSize: 50 * 1024 * 1024,
        // 并发数
        threads: 10
    });

    $("#ctlBtn").on("click", function () {
        uploader.upload();
    });

    // 当有文件被添加进队列的时候
    uploader.on('fileQueued', function (file) {
        $("#thelist").append("<div id=\"" + file.id + "\" class=\"item\">" +
            "<h4 class=\"info\">" + file.name + "</h4>" +
            "<p class=\"state\">等待上传...</p>" +
            "</div>");
        // //暂停上传的文件
        // $("#thelist").on('click', '.stop', function () {
        //     uploader.stop(true);
        // }),
        // //删除上传的文件
        // $("#thelist").on('click', '.remove', function () {
        //     if ($(this).parents(".item").attr('id') === file.id) {
        //         uploader.removeFile(file);
        //         $(this).parents(".item").remove();
        //     }
        // })
    });

    // 文件上传过程中创建进度条实时显示。
    uploader.on('uploadProgress', function (file, percentage) {
        var $li = $('#' + file.id);
        var $percent = $li.find('.progress .progress-bar');
        // 避免重复创建
        if (!$percent.length) {
            // $percent = $('<div class="progress progress-striped active">' +
            //     '<div class="progress-bar" role="progressbar" style="width: 0%">' +
            //     '</div>' +
            //     '</div>').appendTo($li).find('.progress-bar');
            $percent = $('<div class="progress progress-striped active">' +
                '<div class="progress-bar" role="progressbar" style="width: 0">' +
                '</div>' +
                '</div>').appendTo($li).find('.progress');
        }
        $li.find('p.state').text('上传中');
        $percent.css({'width': percentage * 100 + '%', 'height': 10, 'background-color': 'blue'});
    });

    uploader.on('uploadSuccess', function (file) {
        $('#' + file.id).find('p.state').text('已上传');
    });
    uploader.on('uploadError', function (file) {
        $('#' + file.id).find('p.state').text('上传出错');
    });
    uploader.on('uploadComplete', function (file) {
        $('#' + file.id).find('.progress').fadeOut();
    });
});