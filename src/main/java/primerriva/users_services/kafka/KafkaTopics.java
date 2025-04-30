package primerriva.users_services.kafka;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class KafkaTopics {
    public static final String GET_USER_BY_USERNAME = "request-get-user-by-username-topic";
    public static final String CREATE_USER = "request-user-create-topic";
    public static final String GET_ONE_USER = "request-user-get-by-id-topic";
    public static final String UPDATE_USER = "request-user-updated-topic";
    public static final String DELETE_USER = "request-user-deleted-topic";
}
