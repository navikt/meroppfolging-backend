DROP TABLE IF EXISTS FORM_RESPONSE;
DROP TABLE IF EXISTS QUESTION_RESPONSE;

CREATE TABLE FORM_RESPONSE
(
    uuid         UUID PRIMARY KEY,
    person_ident VARCHAR(11) NOT NULL,
    created_at   TIMESTAMP   NOT NULL,
    form_type    TEXT        NOT NULL
);

CREATE TABLE QUESTION_RESPONSE
(
    uuid          UUID PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL,
    question_type TEXT      NOT NULL,
    question_text TEXT      NOT NULL,
    answer_type   TEXT      NOT NULL,
    answer_text   TEXT      NOT NULL,
    response_id   UUID references FORM_RESPONSE (uuid)
);

CREATE INDEX question_id_index ON QUESTION_RESPONSE (question_type);
CREATE INDEX answer_id_index ON QUESTION_RESPONSE (answer_type);
