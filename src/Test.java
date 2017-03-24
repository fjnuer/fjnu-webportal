import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {
    private static List<Cookie> cookieStore = new ArrayList<>();

    public static void main(String[] args) {
        //login("123", "123");
    }

    public static void login(String username, String password) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        cookieStore = cookies;
                    }

                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = cookieStore;
                        return cookies != null ? cookies : new ArrayList<Cookie>();
                    }
                })
                .build();

        //获取登录链接
        Request request = new Request.Builder()
                .url("http://www.msftconnecttest.com/redirect")
                .build();
        Response response = null;
        String body = null;
        try {
            response = okHttpClient.newCall(request).execute();
            body = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!body.contains("10.255.1.1")) {
            System.out.println("已经登录");
            return;
        }

        //解析链接
        body = body.substring("<script>top.self.location.href='".length(), body.length() - "'</script>\r\n".length());
        String loginUrl = body.replaceFirst("http://10.255.1.1:8080/zportal/login", "http://10.255.1.1:8080/zportal/loginForWeb");

        //这一步主要用于获取cookie 不过实际不需要
        request = new Request.Builder()
                .url(body)
                .build();
        try {
            response = okHttpClient.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //解析url中的参数
        String argString = loginUrl.substring(loginUrl.indexOf("?") + 1, loginUrl.length());
        Map<String, String> arg = urlArg(argString);

        //构造post数据
        RequestBody req = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8"), "qrCodeId=请输入编号&username=" + username + "&pwd=" + password + "&validCode=验证码&validCodeFlag=false" + "&ssid=" + arg.get("ssid") + "&mac=" + arg.get("mac") + "&t=" + arg.get("t") + "&wlanacname=" + arg.get("wlanacname") + "&url=" + arg.get("url") + "&nasip=" + arg.get("nasip") + "&wlanuserip=" + arg.get("wlanuserip"));

        //执行登录
        request = new Request.Builder()
                .url("http://10.255.1.1:8080/zportal/login/do")
                .addHeader("Referer", loginUrl)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Origin", "http://10.255.1.1:8080")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.104 Safari/537.36 Core/1.53.2345.400 QQBrowser/9.5.10522.400")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.8")
                .addHeader("Accept", "*/*")
                .post(req)
                .build();
        try {
            response = okHttpClient.newCall(request).execute();
            body = response.body().string();
            body = body.substring(body.indexOf("{\"message\":\"") + "{\"message\":\"".length(), body.indexOf("\",\"nextPage\""));
            if (body.equals("")) {
                System.out.println("登录成功");
            } else {
                System.out.println(body);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logout() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .build();
        Request request = new Request.Builder()
                .url("http://10.255.1.1:8080/zportal/logout")
                .build();
        Response response = null;
        String body = null;
        try {
            response = okHttpClient.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, String> urlArg(String queryString) {
        String[] queryStringSplit = queryString.split("&");
        Map<String, String> queryStringMap =
                new HashMap<String, String>(queryStringSplit.length);
        String[] queryStringParam;
        for (String qs : queryStringSplit) {
            queryStringParam = qs.split("=");
            if (queryStringParam.length > 1)
                queryStringMap.put(queryStringParam[0], queryStringParam[1]);
        }
        return queryStringMap;
    }
}
