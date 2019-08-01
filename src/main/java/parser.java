//import com.google.gson.*;
//import com.google.gson.reflect.TypeToken;
import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.enums.UsersSort;
import com.vk.api.sdk.objects.users.Fields;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
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
        switch (param) {
            case ("country"):
                search_resp = vk.users().search(APP).fields(userFieldList).country(Integer.valueOf(value)).sort(UsersSort.BY_DATE_REGISTERED).count(number).executeAsRaw();
                break;
            case ("city"):
                ClientResponse response = vk.database().getCities(APP, 1).q(value).executeAsRaw();
                System.out.println(value);
                JSONObject City_parse = new JSONObject(response.getContent());
                System.out.println(City_parse);
                JSONObject arr = City_parse.getJSONObject("response").getJSONArray("items").getJSONObject(0);
                int id = arr.getInt("id");
                System.out.println(id);
                search_resp = vk.users().search(APP).fields(userFieldList).city(id).count(number).executeAsRaw();
                break;
            case ("university"):
                search_resp = vk.users().search(APP).fields(userFieldList).university(Integer.valueOf(value)).count(number).executeAsRaw();
                break;
            case ("birth_year"):
                search_resp = vk.users().search(APP).fields(userFieldList).birthYear(Integer.valueOf(value)).count(number).executeAsRaw();
                break;
            case ("school"):
                search_resp = vk.users().search(APP).fields(userFieldList).school(Integer.valueOf(value)).count(number).executeAsRaw();
                break;
            case ("group_id"):
                search_resp = vk.users().search(APP).fields(userFieldList).groupId(Integer.valueOf(value)).count(number).executeAsRaw();
                break;
            default:
                break;
        }

        JSONObject response = new JSONObject(search_resp.getContent());
        return response.getJSONObject("response").getJSONArray("items");
    }

    public static void main(String[] args) throws InterruptedException {
        ////////////////////////////////////////////////

        JSONArray result = new JSONArray();

        JSONArray searching_people = null;
        try {
            searching_people = search(1, "city", "Барнаул");
        } catch (ClientException e) {
            e.printStackTrace();
        }
        assert searching_people != null;
        JSONArray first_iter = getUsers(searching_people);

        for (int i=0;i<first_iter.length();i++) {
            result.put(first_iter.getJSONObject(i));
        }

        System.out.println(first_iter.length());


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

