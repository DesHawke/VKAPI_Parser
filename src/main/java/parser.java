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
    private VkApiClient vk;


    public static void main(String[] args){
        List<Fields> userFieldList = Arrays.asList(
                Fields.SEX,
                Fields.CITY,
                Fields.COUNTRY,
                Fields.PHOTO_50,
                Fields.LAST_SEEN,
                Fields.UNIVERSITIES,
                Fields.SCHOOLS);

    TransportClient transportClient = HttpTransportClient.getInstance();
    VkApiClient vk = new VkApiClient(transportClient);

    int app_id=7058737;
    String access_token="779d5a5b2c5be69b6d134f734a2fbb51b087520d8aa948680eb021448801cc91ea172e19041c2d7be3e9c";

    UserActor APP= new UserActor(app_id,access_token);

    JsonArray first_arr=new JsonArray();
    try {
        String first_people = vk.users().search(APP).sort(UsersSort.BY_DATE_REGISTERED).fields(userFieldList).universityYear(2014).count(5).executeAsString();
        JsonObject converted=new JsonParser().parse(first_people).getAsJsonObject();
        JsonArray conv2= converted.getAsJsonObject("response").getAsJsonArray("items");

        if (conv2 != null) {
            int len = conv2.size();
            //System.out.println(len);
            for (int i = 0; i < len; i++) {
                conv2.get(i).getAsJsonObject().remove("track_code");

                boolean is_deactivated=conv2.get(i).getAsJsonObject().has("deactivated");
                if(!is_deactivated) {
                    boolean is_closed=conv2.get(i).getAsJsonObject().get("is_closed").getAsBoolean();
                    if (!is_closed) {
                        JsonElement test_id = conv2.get(i).getAsJsonObject().get("id");
                        System.out.println(test_id);
                        String friends = vk.friends().get(APP)
                                .userId(test_id.getAsInt())
                                .executeAsString();
                        Thread.sleep(500);
                        JsonObject friends_obj = new JsonParser().parse(friends).getAsJsonObject();
                        JsonArray friends_arr = friends_obj.getAsJsonObject("response").getAsJsonArray("items");
                        conv2.get(i).getAsJsonObject().add("friends", friends_arr);
                        conv2.get(i).getAsJsonObject().remove("is_closed");
                        conv2.get(i).getAsJsonObject().remove("can_access_closed");
                        first_arr.add(conv2.get(i).getAsJsonObject());
                    }
                }
            }
        }
    } catch (ClientException | InterruptedException e) {
        e.printStackTrace();
    }

        ArrayList<JsonArray> second_iter= new ArrayList<>();
    second_iter.add(first_arr);
    if (first_arr != null) {
        int len = first_arr.size();
        //System.out.println(len);
        for (int i = 0; i < len; i++) {
        {
            JsonElement friends1 = first_arr.get(i).getAsJsonObject().get("friends");
            Type listType = new TypeToken<List<Integer>>() {}.getType();
            List<Integer> yourList = new Gson().fromJson(friends1, listType);
            JsonArray friends_array=new JsonArray();
            for (int user_id : yourList) {
                try {
                    ClientResponse user_response = vk.users().get(APP)
                        .userIds(Integer.toString(user_id))
                        .fields(userFieldList)
                        .executeAsRaw();
                    Thread.sleep(500);
                    JsonObject converted=new JsonParser().parse(user_response.getContent()).getAsJsonObject();
                    JsonObject user=converted.getAsJsonArray("response").get(0).getAsJsonObject();
                    //System.out.println(user);

                    boolean is_deactivated=user.has("deactivated");
                    if (!is_deactivated) {
                        boolean is_closed=user.get("is_closed").getAsBoolean();
                        if (!is_closed) {
                            JsonElement test_id = user.get("id");
                            ClientResponse friends = vk.friends().get(APP)
                                    .userId(test_id.getAsInt())
                                    .executeAsRaw();
                            Thread.sleep(500);
                            JsonObject friends_obj = new JsonParser().parse(friends.getContent()).getAsJsonObject();
                            JsonArray friends_arr = friends_obj.getAsJsonObject("response").getAsJsonArray("items");
                            user.add("friends", friends_arr);
                            user.remove("is_closed");
                            user.remove("can_access_closed");
                            friends_array.add(user);
                        }
                    }

                } catch (ClientException | InterruptedException e) {
                    e.printStackTrace();
                }

            }
            if(friends_array.size()>0) {
                second_iter.add(friends_array);
            }
            }
        }
//    ClientResponse user_response = vk.users().get(APP)
//            .userIds(Integer.toString(user_id))
//            .fields(userFieldList)
//            .executeAsRaw();
//    ClientResponse friends_response = vk.friends().get(APP)
//            .userId(user_id)
//            .executeAsRaw();
//    System.out.println(user_response.getContent());
//    System.out.println(friends_response.getContent());


    }
        for (JsonArray jsonElements : second_iter) {
            System.out.print(jsonElements.size() + " - ");
            System.out.println(jsonElements);
        }
    }
}
