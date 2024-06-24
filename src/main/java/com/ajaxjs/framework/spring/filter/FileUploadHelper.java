package com.ajaxjs.framework.spring.filter;


import com.ajaxjs.util.StrUtil;
import com.ajaxjs.util.io.FileHelper;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * 文件上传的辅助类
 */
public class FileUploadHelper {
    /**
     * 初始化文件上传
     */
    public static void initUpload(ServletContext cxt, ServletRegistration.Dynamic registration) {
        initUpload(cxt, registration, System.getProperty("java.io.tmpdir") + "upload_dir");
    }

    public static void initUpload(ServletContext cxt, ServletRegistration.Dynamic registration, String uploadDir) {
        // 如果不存在则创建
        FileHelper.mkDir(uploadDir);
        registration.setMultipartConfig(new MultipartConfigElement(uploadDir, 50000000, 50000000, 0));// 文件上传
    }

    /**
     * 文件上传
     */
    public static MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        resolver.setResolveLazily(true);// resolveLazily 属性启用是为了推迟文件解析，以在在 UploadAction 中捕获文件大小异常

        return resolver;
    }

    public static final String CONTENT_TYPE = "multipart/form-data";

    public static final String CONTENT_TYPE2 = "multipart/form-data;charset=UTF-8";

    /**
     * 保存上传的文件
     *
     * @param file          文件
     * @param uploadDir     保存目录
     * @param isNewAutoName 是否重新命名？
     */
    public static String upload(MultipartFile file, String uploadDir, boolean isNewAutoName) {
        Objects.requireNonNull(file);
        String filename = file.getOriginalFilename();

        if (filename == null)
            throw new IllegalArgumentException("表单上传的参数 name 与方法中 MultipartFile 的参数名是否一致?");

        if (isNewAutoName)
            filename = getAutoName(filename);

        FileHelper.mkDir(uploadDir);
        File file2 = new File(uploadDir + filename);

        try {
            file.transferTo(file2);
            boolean v;
            v = file2.setReadable(true, false);
            v = file2.setExecutable(true, false);
            v = file2.setWritable(true, false);
        } catch (IllegalStateException | IOException e) {
            System.err.println("文件上传失败");
            e.printStackTrace();
        }

        return filename;
    }

    /**
     * 根据原始文件名生成自动名称，保留扩展名
     *
     * @param originalFilename 原始文件名
     * @return 自动文件名
     */
    public static String getAutoName(String originalFilename) {
        String[] arr = originalFilename.split("\\.");
        String ext = "";

        if (arr.length >= 2)
            ext = "." + arr[arr.length - 1];
//        else {
//            // 没有扩展名
//        }

//        return SnowflakeId.get() + ext;
        return StrUtil.getRandomString(8) + ext;
    }
}
