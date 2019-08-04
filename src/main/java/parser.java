import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
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

    private static JSONArray getFriends(int id) throws InterruptedException {
        Thread.sleep(350);
        ClientResponse friends_resp = null;
        try {
            friends_resp = vk.friends().get(APP)
                    .userId(id)
                    .executeAsRaw();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        assert friends_resp != null;
        JSONObject friends = new JSONObject(friends_resp.getContent());
        //System.out.println(friends);
        return friends.getJSONObject("response").getJSONArray("items");

    }

    private static JSONArray getUsers(JSONArray array) throws InterruptedException {
        JSONArray Users = new JSONArray();

        for (int i = 0; i < array.length(); i++) {

            // Проверка на забаненность
            boolean is_deactivated = array.getJSONObject(i).has("deactivated");

            if (!is_deactivated) {

                //Проверка на закрытость
                boolean is_closed = array.getJSONObject(i).getBoolean("is_closed");

                if (!is_closed) {
                    JSONObject user = array.getJSONObject(i);
                    JSONArray friends = getFriends(user.getInt("id"));

                    if (friends.length()>0 && friends.length()<1000) {
                        user.remove("is_closed");
                        user.remove("can_access_closed");
                        user.remove("track_code");

                        user.put("friends", friends);
                        Users.put(array.get(i));
                    }
                }
            }
        }
        return Users;
    }

    private static JSONObject getUserInfo(int id) throws InterruptedException {
        Thread.sleep(350);
        ClientResponse user_response = null;
        try {
            user_response = vk.users().get(APP)
                    .userIds(Integer.toString(id))
                    .fields(userFieldList)
                    .executeAsRaw();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        assert user_response != null;
        JSONObject converted = new JSONObject(user_response.getContent());
        JSONObject user = converted.getJSONArray("response").getJSONObject(0);

        boolean is_deactivated = user.has("deactivated");
        if (!is_deactivated) {
            boolean is_closed = user.getBoolean("is_closed");
            if (!is_closed) {

                JSONArray friends_arr = getFriends(user.getInt("id"));
                if (friends_arr.length()>0 && friends_arr.length()<1000) {
                    user.put("friends", friends_arr);
                    user.remove("is_closed");
                    user.remove("can_access_closed");
                    return user;
                }
            }
        }
        return null;
    }

    private static JSONArray search(int number, String param, String value) throws ClientException {
        ClientResponse search_resp = null;
        Scanner in = new Scanner(System.in);
        ClientResponse response;

        switch (param) {
            case ("city"):
                // Наиболее вероятный город
                response = vk.database().getCities(APP, 1).q(value).executeAsRaw();
                JSONObject City_parse = new JSONObject(response.getContent());
                JSONObject arr = City_parse.getJSONObject("response").getJSONArray("items").getJSONObject(0);

                int city_id = arr.getInt("id");
                search_resp = vk.users().search(APP).fields(userFieldList).city(city_id).count(number).executeAsRaw();
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
                search_resp = vk.users().search(APP).fields(userFieldList).university(uni_id).executeAsRaw();
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
                search_resp = vk.users().search(APP).fields(userFieldList).school(school_id).count(number).executeAsRaw();
                break;
            default:
                break;
        }

        JSONObject obj = new JSONObject(search_resp.getContent());
        return obj.getJSONObject("response").getJSONArray("items");
    }

    public static void main(String[] args) throws InterruptedException {


        JSONArray result = new JSONArray();

        //Входные данные
        String param;
        String value;
        int number=1;

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
        assert searching_people != null;
        JSONArray first_iter = getUsers(searching_people);

        for (int i=0;i<first_iter.length();i++) {
            result.put(first_iter.getJSONObject(i));
        }


        if (first_iter != null) {
            int len = first_iter.length();

            for (int i = 0; i < len; i++) {
                {
                    JSONArray friends1 = first_iter.getJSONObject(i).getJSONArray("friends");

                    for (int j=0;j<friends1.length();j++) {
                        JSONObject user = getUserInfo(friends1.getInt(j));
                        if (user != null)
                            result.put(user);
                    }
                }
            }

        }

        for (int i=0;i<result.length();i++) {
            System.out.println(result.get(i));
        }
        System.out.println(result.length());
    }
}

