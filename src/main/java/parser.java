import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.enums.UsersSort;
import com.vk.api.sdk.objects.users.Fields;

import java.lang.reflect.Type;
import java.util.*;


public class parser {
    private static TransportClient transportClient = HttpTransportClient.getInstance();
    private static VkApiClient vk = new VkApiClient(transportClient);

    private static int app_id = 7058737;
    private static String access_token = "779d5a5b2c5be69b6d134f734a2fbb51b087520d8aa948680eb021448801cc91ea172e19041c2d7be3e9c";

    private static UserActor APP = new UserActor(app_id, access_token);

    private static List<Fields> userFieldList = Arrays.asList(
            Fields.SEX,
            Fields.CITY,
            Fields.COUNTRY,
            Fields.PHOTO_50,
            Fields.LAST_SEEN,
            Fields.UNIVERSITIES,
            Fields.SCHOOLS);

    private static JsonArray getFriends(int id) throws InterruptedException {
        Thread.sleep(350);
        ClientResponse friends_resp = null;
        try {
            friends_resp = vk.friends().get(APP)
                    .userId(id)
                    .executeAsRaw();
        } catch (ClientException e) {
            e.printStackTrace();
        }
        //Thread.sleep(500);
        JsonObject friends = new JsonParser().parse(friends_resp.getContent()).getAsJsonObject();
        int check=friends.getAsJsonObject("response").get("count").getAsInt();
        if((check < 1000) && (check > 0)) {
            return friends.getAsJsonObject("response").getAsJsonArray("items");
        }
        else
            return null;
    }

    private static JsonArray getUsers(JsonArray array) throws InterruptedException {
        int len = array.size();
        JsonArray Users = new JsonArray();

        for (int i = 0; i < len; i++) {

            // Проверка на забаненность
            boolean is_deactivated = array.get(i).getAsJsonObject().has("deactivated");

            if (!is_deactivated) {

                //Проверка на закрытость
                boolean is_closed = array.get(i).getAsJsonObject().get("is_closed").getAsBoolean();

                if (!is_closed) {
                    JsonObject user = array.get(i).getAsJsonObject();
                    JsonArray friends=getFriends(user.get("id").getAsInt());
                    if (friends!=null) {
                        user.remove("is_closed");
                        user.remove("can_access_closed");
                        user.remove("track_code");
                        user.add("friends", getFriends(user.get("id").getAsInt()));
                        Users.add(array.get(i).getAsJsonObject());
                    }
                }
            }
        }
        return Users;
    }

    private static JsonObject getUserInfo(int id) throws InterruptedException {
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
        JsonObject converted = new JsonParser().parse(user_response.getContent()).getAsJsonObject();
        JsonObject user = converted.getAsJsonArray("response").get(0).getAsJsonObject();

        boolean is_deactivated = user.has("deactivated");
        if (!is_deactivated) {
            boolean is_closed = user.get("is_closed").getAsBoolean();
            if (!is_closed) {

                JsonArray friends_arr = getFriends(user.get("id").getAsInt());
                if (friends_arr!=null) {
                    user.add("friends", friends_arr);
                    user.remove("is_closed");
                    user.remove("can_access_closed");
                    return user;
                }
            }
        }
        return null;
    }

    private static JsonArray search(int number, String param, String value) throws ClientException {
        ClientResponse search_resp = null;
        switch (param){
            case ("country"):
                search_resp = vk.users().search(APP).fields(userFieldList).country(Integer.valueOf(value)).count(number).executeAsRaw();
            break;
            case ("city"):
                ClientResponse response = vk.database().getCities(APP, 1).q(value).executeAsRaw();
                JsonObject City_parse = new JsonParser().parse(response.getContent()).getAsJsonObject();
                JsonObject arr = City_parse.getAsJsonObject("response").getAsJsonArray("items").get(0).getAsJsonObject();
                JsonElement id= arr.get("id");
                search_resp = vk.users().search(APP).fields(userFieldList).city(Integer.valueOf(value)).count(number).executeAsRaw();
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

        JsonObject response = new JsonParser().parse(search_resp.getContent()).getAsJsonObject();
        JsonArray users_arr = response.getAsJsonObject("response").getAsJsonArray("items");
        return users_arr;
    }

    public static void main(String[] args) throws InterruptedException {
        ////////////////////////////////////////////////

        ArrayList<JsonArray> result = new ArrayList<>();


        JsonArray searching_people = null;
        try {
            searching_people = search(1,"country","1");
        } catch (ClientException e) {
            e.printStackTrace();
        }
        assert searching_people != null;
        JsonArray first_iter = getUsers(searching_people);

        result.add(first_iter);

        if (first_iter != null) {
            int len = first_iter.size();

            for (int i = 0; i < len; i++) {
                {
                    JsonElement friends1 = first_iter.get(i).getAsJsonObject().get("friends");
                    Type listType = new TypeToken<List<Integer>>() {
                    }.getType();
                    List<Integer> yourList = new Gson().fromJson(friends1, listType);
                    JsonArray friends_array = new JsonArray();
                    for (int user_id : yourList) {
                        JsonObject user=getUserInfo(user_id);
                        if (user!=null)
                        friends_array.add(user);
                    }
                    if (friends_array.size() > 0) {
                        result.add(friends_array);
                        System.out.println("added="+friends_array.size());
                    }
                }
            }

        }

        int sum=0;
        for (JsonArray jsonElements : result) {
            System.out.println(jsonElements.size());
            sum+=jsonElements.size();
            //System.out.println(jsonElements);
        }
        System.out.println(sum);

    }
}

