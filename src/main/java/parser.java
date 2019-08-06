import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.actions.Execute;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.users.Fields;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;


public class parser {
    private static TransportClient transportClient = HttpTransportClient.getInstance();
    private static VkApiClient vk = new VkApiClient(transportClient);

    private static int app_id = 7058737;
    private static String access_token = "779d5a5b2c5be69b6d134f734a2fbb51b087520d8aa948680eb021448801cc91ea172e19041c2d7be3e9c";
    //private static String access_token = "6a99b6dce7c59daac6a76e3703ab3b75b6ddc9f4ee569c9ef1173c07cee4638d2c47e9eab2c22753863af";
    private static UserActor APP = new UserActor(app_id, access_token);

    private static List<Fields> userFieldList = Arrays.asList(
            Fields.SEX,
            Fields.CITY,
            Fields.COUNTRY,
            Fields.PHOTO_50,
            Fields.LAST_SEEN,
            Fields.UNIVERSITIES,
            Fields.SCHOOLS);

    private static JSONObject getUser(int user_id) throws ClientException, InterruptedException {
        String code=
                "var info= API.users.get({\"user_ids\":" + user_id + ",\"fields\": \"sex, city, country, photo_50,last_seen,universities,school\"});" +
                "var is_deactivated=info@.is_deactivated;"+
                "if(is_deactivated!=null){"+
                    "var is_closed=info@.is_closed;"+
                    "if(is_closed){"+
                        "var friends=API.friends.get({\"user_id\":" + user_id + "});"+
                        "if(friends.count>0 && friends.count<1000){"+
                            "var result={id:info[0].id,"+
                                        "first_name:info[0].first_name,"+
                                        "last_name:info[0].last_name,"+
                                        "sex:info[0].sex,"+
                                        "city:info[0].city,"+
                                        "country:info[0].country,"+
                                        "photo_50:info[0].photo_50,"+
                                        "last_seen:info[0].last_seen,"+
                                        "universities:info[0].universities,"+
                                        "schools:info[0].schools,"+
                                        "friends:friends.items };"+
                            "return result;}}}" +
                        "else return null;";

        Thread.sleep(350);
        ClientResponse resp=vk.execute().code(APP,code).executeAsRaw();

        JSONObject user = new JSONObject(resp.getContent());
        if (!user.isNull("response")) {
            return user.getJSONObject("response");
        }
        else return null;
    }

    private static JSONArray getUsersArr(JSONArray users) throws ClientException, InterruptedException {
        JSONArray result=new JSONArray();
        for (int i=0;i<users.length();i++) {
            JSONObject user = getUser(users.getInt(i));
            if (user!=null)
                result.put(user);
        }
        return result;
    }

    private static JSONArray search(int number, String param, String value) throws ClientException {
        ClientResponse search_resp = null;
        Scanner in = new Scanner(System.in);
        ClientResponse response;
        String code;
        switch (param) {
            case ("city"):
                // Наиболее вероятный город
                response = vk.database().getCities(APP, 1).q(value).executeAsRaw();
                JSONObject City_parse = new JSONObject(response.getContent());
                JSONObject arr = City_parse.getJSONObject("response").getJSONArray("items").getJSONObject(0);

                int city_id = arr.getInt("id");
                code="var result= API.users.search({\"city\":"+city_id+",\"count\":"+number+"});\n" +
                        "return result.items@.id;";
                search_resp=vk.execute().code(APP,code).executeAsRaw();
                break;

            case ("university"):
                response = vk.database().getUniversities(APP).q(value).count(5).executeAsRaw();
                JSONObject uni_parse = new JSONObject(response.getContent());
                JSONArray array = uni_parse.getJSONObject("response").getJSONArray("items");

                System.out.println("Выберите нужный университет по ID:");

                for (int i=0;i<array.length();i++) {
                    System.out.println("ID " + array.getJSONObject(i).get("id") + "\t" +array.getJSONObject(i).get("title"));
                }
                int uni_id = in.nextInt();
                code="var result= API.users.search({\"university\":"+uni_id+",\"count\":"+number+"});\n" +
                        "return result.items@.id;";
                search_resp=vk.execute().code(APP,code).executeAsRaw();
                break;

            case ("school"):
                System.out.println("Выберите город:");
                String name=in.nextLine();
                ClientResponse city_response = vk.database().getCities(APP, 1).q(name).executeAsRaw();
                JSONObject city_school = new JSONObject(city_response.getContent());
                JSONObject city_school_arr = city_school.getJSONObject("response").getJSONArray("items").getJSONObject(0);

                int city_school_id = city_school_arr.getInt("id");
                response = vk.database().getSchools(APP, city_school_id).q(value).count(5).executeAsRaw();
                JSONObject school_parse = new JSONObject(response.getContent());
                JSONArray school_array = school_parse.getJSONObject("response").getJSONArray("items");

                System.out.println("Выберите нужную школу по ID:");
                for (int i=0;i<school_array.length();i++) {
                    System.out.println("ID " + school_array.getJSONObject(i).get("id") + "\t" +school_array.getJSONObject(i).get("title"));
                }
                int school_id = in.nextInt();
                code="var result= API.users.search({\"school\":"+school_id+",\"count\":"+number+"});\n" +
                        "return result.items@.id;";
                search_resp=vk.execute().code(APP,code).executeAsRaw();
                break;
            default:
                break;
        }

        JSONObject obj = new JSONObject(search_resp.getContent());
        return obj.getJSONArray("response");
    }

    public static void main(String[] args) throws InterruptedException, ClientException {

        JSONArray result = new JSONArray();

        //Входные данные
        String param;
        String value;
        int number=20;

        // ! ПРОБЛЕМЫ С РУССКОЙ КОДИРОВКОЙ !
        Scanner in = new Scanner(System.in);
        System.out.println("Введите параметр: ");
        param = in.nextLine();
        System.out.println("Input a value: ");
        value = in.nextLine();

        JSONArray searching_people = null;
        try {
            searching_people = search(number, param, value);
        } catch (ClientException e) {
            e.printStackTrace();
        }

        JSONArray first_iter=getUsersArr(searching_people);

        for (int i=0;i<first_iter.length();i++) {
            result.put(first_iter.getJSONObject(i));
        }

        int len = first_iter.length();

        for (int i = 0; i < len; i++) {
            JSONArray friends_list = first_iter.getJSONObject(i).getJSONArray("friends");
            JSONArray second_iter=(getUsersArr(friends_list));
            for (int j=0;j<second_iter.length();j++) {
                result.put(second_iter.getJSONObject(j));
            }
        }
        for (int i=0;i<result.length();i++) {
            System.out.println(result.get(i));
        }
        System.out.println(result.length());
    }
}

