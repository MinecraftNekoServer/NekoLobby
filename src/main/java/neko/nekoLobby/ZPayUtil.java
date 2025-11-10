package neko.nekoLobby;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ZPayUtil {
    
    // Z-Pay接口配置
    private String apiUrl = "https://zpayz.cn/";
    private String pid; // 商户ID
    private String key; // 商户密钥
    private String notifyUrl; // 异步通知地址
    private String returnUrl; // 同步跳转地址
    
    public ZPayUtil(String pid, String key, String notifyUrl, String returnUrl) {
        this.pid = pid;
        this.key = key;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
    }
    
    /**
     * 生成商户订单号
     */
    public String generateOrderNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String date = sdf.format(new Date());
        Random random = new Random();
        int rand = random.nextInt(900) + 100; // 生成3位随机数
        return date + rand;
    }
    
    /**
     * MD5签名算法
     */
    public String sign(Map<String, String> params) {
        // 1. 按参数名ASCII码从小到大排序（a-z），sign、sign_type、和空值不参与签名
        String signStr = params.entrySet().stream()
            .filter(entry -> !entry.getKey().equals("sign") && 
                           !entry.getKey().equals("sign_type") && 
                           entry.getValue() != null && 
                           !entry.getValue().isEmpty())
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));
        
        // 2. 将拼接好的字符串与商户密钥KEY进行MD5加密
        signStr += key;
        return md5(signStr).toLowerCase();
    }
    
    /**
     * MD5加密
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 创建支付订单 - API方式
     */
    public Map<String, Object> createPaymentOrder(String orderNo, String name, String money, String type, String ip, String param) {
        try {
            String url = apiUrl + "mapi.php";
            
            Map<String, String> params = new HashMap<>();
            params.put("pid", pid);
            params.put("type", type); // 支付方式: alipay, wxpay
            params.put("out_trade_no", orderNo);
            params.put("notify_url", notifyUrl);
            params.put("name", name);
            params.put("money", money);
            params.put("clientip", ip);
            params.put("param", param != null ? param : "");
            params.put("sign_type", "MD5");
            
            // 生成签名
            String sign = sign(params);
            params.put("sign", sign);
            
            // 构建POST请求参数
            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> paramEntry : params.entrySet()) {
                if (postData.length() != 0) {
                    postData.append('&');
                }
                postData.append(URLEncoder.encode(paramEntry.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(paramEntry.getValue(), "UTF-8"));
            }
            
            // 发送POST请求
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "ZPay Client");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Accept", "application/json");
            
            con.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.writeBytes(postData.toString());
                wr.flush();
            }
            
            int responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            // 解析响应
            String responseStr = response.toString();
            return parseJson(responseStr);
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("创建支付订单失败: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", "error");
            errorResult.put("msg", "网络请求失败: " + e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * 获取支付二维码链接
     */
    public String getPaymentQRCodeUrl(String orderNo, String name, String money, String type, String ip, String param) {
        try {
            String url = apiUrl + "mapi.php";
            
            Map<String, String> params = new HashMap<>();
            params.put("pid", pid);
            params.put("type", type); // 支付方式: alipay, wxpay
            params.put("out_trade_no", orderNo);
            params.put("notify_url", notifyUrl);
            params.put("name", name);
            params.put("money", money);
            params.put("clientip", ip);
            params.put("param", param != null ? param : "");
            params.put("sign_type", "MD5");
            
            // 生成签名
            String sign = sign(params);
            params.put("sign", sign);
            
            // 构建POST请求参数
            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> paramEntry : params.entrySet()) {
                if (postData.length() != 0) {
                    postData.append('&');
                }
                postData.append(URLEncoder.encode(paramEntry.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(paramEntry.getValue(), "UTF-8"));
            }
            
            // 发送POST请求
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "ZPay Client");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Accept", "application/json");
            
            con.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.writeBytes(postData.toString());
                wr.flush();
            }
            
            int responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            // 解析响应
            String responseStr = response.toString();
            Bukkit.getLogger().info("[ZPayUtil] 收到的原始响应: " + responseStr);
            
            // 检查是否是被引号包围的JSON字符串
            String processedResponse = responseStr.trim();
            if (processedResponse.startsWith("\"") && processedResponse.endsWith("\"")) {
                // 移除外层引号并处理转义字符
                processedResponse = processedResponse.substring(1, processedResponse.length() - 1);
                processedResponse = processedResponse.replace("\\\"", "\"").replace("\\\\", "\\");
                Bukkit.getLogger().info("[ZPayUtil] 处理后的响应: " + processedResponse);
            }
            
            Map<String, Object> result = parseJson(processedResponse);
            Bukkit.getLogger().info("[ZPayUtil] 解析后的结果: " + result);
            
            if (result != null && result.containsKey("code") && "1".equals(result.get("code").toString())) {
                Bukkit.getLogger().info("[ZPayUtil] 支付订单创建成功");
                // 根据您的说明，只有img才是付款二维码的url
                if (result.containsKey("img")) {
                    String imgUrl = (String) result.get("img");
                    // 处理URL中的转义字符
                    imgUrl = imgUrl.replace("\\/", "/");
                    Bukkit.getLogger().info("[ZPayUtil] 使用img字段作为二维码URL: " + imgUrl);
                    return imgUrl;
                } else {
                    Bukkit.getLogger().warning("[ZPayUtil] 响应中未找到img字段");
                    Bukkit.getLogger().info("[ZPayUtil] 可用字段: " + result.keySet());
                }
            } else {
                Bukkit.getLogger().warning("[ZPayUtil] 支付订单创建失败或响应格式不正确");
                Bukkit.getLogger().info("[ZPayUtil] 响应码: " + (result != null ? result.get("code") : "null"));
            }
            
            return null;
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("获取支付二维码失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 验证支付回调签名
     */
    public boolean verifySign(Map<String, String> params) {
        if (!params.containsKey("sign")) {
            return false;
        }
        
        String sign = params.get("sign");
        params.remove("sign"); // 移除sign参数用于验证
        
        String expectedSign = sign(params);
        return expectedSign.equals(sign);
    }
    
    /**
     * 解析JSON响应
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String jsonStr) {
        // 简单的JSON解析，实际项目中建议使用专门的JSON库
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 移除首尾的花括号和可能的引号
            jsonStr = jsonStr.trim();
            if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
                // 处理转义字符
                jsonStr = jsonStr.replace("\\\"", "\"");
                jsonStr = jsonStr.replace("\\\\", "\\");
            }
            
            if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
                jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
            }
            
            // 按逗号分割，但要处理引号内的逗号
            List<String> pairs = new ArrayList<>();
            int braceLevel = 0;
            boolean inQuotes = false;
            StringBuilder currentPair = new StringBuilder();
            
            for (int i = 0; i < jsonStr.length(); i++) {
                char c = jsonStr.charAt(i);
                
                if (c == '"' && (i == 0 || jsonStr.charAt(i-1) != '\\')) {
                    inQuotes = !inQuotes;
                } else if (c == '{' && !inQuotes) {
                    braceLevel++;
                } else if (c == '}' && !inQuotes) {
                    braceLevel--;
                }
                
                if (c == ',' && braceLevel == 0 && !inQuotes) {
                    pairs.add(currentPair.toString().trim());
                    currentPair.setLength(0); // 清空StringBuilder
                } else {
                    currentPair.append(c);
                }
            }
            
            if (currentPair.length() > 0) {
                pairs.add(currentPair.toString().trim());
            }
            
            for (String pair : pairs) {
                pair = pair.trim();
                if (pair.isEmpty()) continue;
                
                // 查找冒号，但要跳过在引号内的冒号
                int colonIndex = -1;
                boolean inQuote = false;
                for (int i = 0; i < pair.length(); i++) {
                    if (pair.charAt(i) == '"' && (i == 0 || pair.charAt(i-1) != '\\')) {
                        inQuote = !inQuote;
                    } else if (pair.charAt(i) == ':' && !inQuote) {
                        colonIndex = i;
                        break;
                    }
                }
                
                if (colonIndex == -1) continue;
                
                String key = pair.substring(0, colonIndex).trim();
                String value = pair.substring(colonIndex + 1).trim();
                
                // 移除键的引号
                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                    // 处理键中的转义字符
                    key = key.replace("\\\"", "\"");
                    key = key.replace("\\\\", "\\");
                }
                
                // 移除值的引号
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                    // 处理值中的转义字符
                    value = value.replace("\\\"", "\"");
                    value = value.replace("\\\\", "\\");
                }
                
                result.put(key, value);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[ZPayUtil] JSON解析错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
}