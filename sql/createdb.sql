
-- Create tables

CREATE TABLE Person (
    ID             VARCHAR(50) NOT NULL,
    full_name      VARCHAR(50) NOT NULL,
    gender         CHAR(1),
    birthdate      DATE,
    birth_town     VARCHAR(25),
    birth_province VARCHAR(25),
    birth_state    VARCHAR(25),
    birth_nation   VARCHAR(25),
    is_director    NUMBER(1),
    is_actor       NUMBER(1),
    PRIMARY KEY (ID)
);

CREATE TABLE Guardian (
    minor_id       VARCHAR(50) NOT NULL,
    adult_id       VARCHAR(50) NOT NULL,
    CONSTRAINT pk_guardian
        PRIMARY KEY (minor_id, adult_id),
    CONSTRAINT fk_guardian_minor
        FOREIGN KEY (minor_id) REFERENCES Person(ID)
        ON DELETE CASCADE,
    CONSTRAINT fk_guardian_adult
        FOREIGN KEY (adult_id) REFERENCES Person(ID)
        ON DELETE CASCADE
);

CREATE TABLE Marriage (
    husband_id      VARCHAR(50) NOT NULL,
    wife_id         VARCHAR(50) NOT NULL,
    year            NUMBER(4),
    CONSTRAINT pk_marriage
        PRIMARY KEY (husband_id, wife_id),
    CONSTRAINT fk_marriage_husband
        FOREIGN KEY (husband_id) REFERENCES Person(ID)
        ON DELETE CASCADE,
    CONSTRAINT fk_marriage_wife
        FOREIGN KEY (wife_id) REFERENCES Person(ID)
        ON DELETE CASCADE
);

CREATE TABLE Company (
    name         VARCHAR(20) NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE Movie (
    ID          NUMBER NOT NULL,
    title       VARCHAR(200) NOT NULL,
    year        NUMBER(4) NOT NULL,
    director_id VARCHAR(50),
    cost        NUMBER(12),
    category    VARCHAR(10),
    is_comedy   NUMBER(1),
    is_drama    NUMBER(1),
    is_action   NUMBER(1),
    company     VARCHAR(10),
    contract_no VARCHAR(10),
    all_critics_rating       NUMBER(3,1),
    all_critics_num_reviews  NUMBER(10),
    top_critics_rating       NUMBER(3,1),
    top_critics_num_reviews  NUMBER(10),
    audience_rating          NUMBER(3,1),
    audience_num_ratings     NUMBER(10),
    CONSTRAINT pk_movie
        PRIMARY KEY (ID),
    CONSTRAINT fk_director
        FOREIGN KEY (director_id) REFERENCES Person(ID),
    CONSTRAINT fk_producer
        FOREIGN KEY (company) REFERENCES Company(name)
        ON DELETE CASCADE
);

CREATE TABLE Genres (
    movie_id    NUMBER NOT NULL,
    genre       VARCHAR(25) NOT NULL,
    CONSTRAINT pk_genre
        PRIMARY KEY (movie_id, genre),
    CONSTRAINT fk_movie_genres
        FOREIGN KEY (movie_id) REFERENCES Movie(ID)
        ON DELETE CASCADE
);

CREATE TABLE Cast (
    movie_id    NUMBER NOT NULL,
    actor_id    VARCHAR(50) NOT NULL,
    ranking     NUMBER(4),
    role        VARCHAR(20),
    CONSTRAINT pk_cast
        PRIMARY KEY (movie_id, actor_id, ranking),
    CONSTRAINT fk_cast_movie
        FOREIGN KEY (movie_id) REFERENCES Movie(ID) 
        ON DELETE CASCADE,
    CONSTRAINT fk_cast_actor
        FOREIGN KEY (actor_id) REFERENCES Person(ID) 
        ON DELETE CASCADE
);

CREATE TABLE MovieCountry (
    ID          NUMBER NOT NULL,
    movie_id    NUMBER NOT NULL,
    country     VARCHAR(50),
    CONSTRAINT pk_country
        PRIMARY KEY (ID),
    CONSTRAINT fk_country_movie
        FOREIGN KEY (movie_id) REFERENCES Movie(ID) 
        ON DELETE CASCADE   
);

CREATE TABLE MovieLocation (
    ID          NUMBER NOT NULL,
    movie_id    NUMBER NOT NULL,
    country     VARCHAR(50),
    state       VARCHAR(100),
    city        VARCHAR(100),
    street      VARCHAR(200),
    CONSTRAINT pk_location
        PRIMARY KEY (ID),
    CONSTRAINT fk_location_movie
        FOREIGN KEY (movie_id) REFERENCES Movie(ID) 
        ON DELETE CASCADE   
);

CREATE TABLE TVSeries (
    ID          NUMBER NOT NULL,
    title       VARCHAR(200) NOT NULL,    
    tv_network  VARCHAR(10) NOT NULL,
    company     VARCHAR(10),
    contract_no VARCHAR(10),
    CONSTRAINT pk_tvseries
        PRIMARY KEY (ID),
    CONSTRAINT fk_tvseries_producer
        FOREIGN KEY (company) REFERENCES Company(name)
        ON DELETE CASCADE
);

CREATE TABLE Scene (
    ID          NUMBER NOT NULL,
    scene_no    VARCHAR(10) NOT NULL,
    CONSTRAINT pk_scene
        PRIMARY KEY (ID, scene_no),
    CONSTRAINT fk_movie_scene
        FOREIGN KEY (ID) REFERENCES Movie(ID)
        ON DELETE CASCADE
);

CREATE TABLE Episode (
    ID          NUMBER NOT NULL,
    ep_no       VARCHAR(10) NOT NULL,
    title       VARCHAR(50) NOT NULL,
    length      NUMBER      NOT NULL,
    CONSTRAINT pk_episode
        PRIMARY KEY (ID, ep_no),
    CONSTRAINT fk_series_episode 
        FOREIGN KEY (ID) REFERENCES TVSeries(ID)
        ON DELETE CASCADE
);

CREATE TABLE Guest (
    ID          NUMBER NOT NULL,
    actor_id    VARCHAR(50) NOT NULL,
    CONSTRAINT pk_guest
        PRIMARY KEY (ID, actor_id),
    CONSTRAINT fk_guest_series
        FOREIGN KEY (ID) REFERENCES TVSeries(ID) 
        ON DELETE CASCADE,
    CONSTRAINT fk_guest_actor
        FOREIGN KEY (actor_id) REFERENCES Person(ID) 
        ON DELETE CASCADE
);

CREATE TABLE Nomination (
    ID        VARCHAR(10) NOT NULL,
    award       VARCHAR(20) NOT NULL,
    event       VARCHAR(20) NOT NULL,
    category    VARCHAR(20) NOT NULL,
    PRIMARY KEY (ID)
);

CREATE TABLE NomineeMovie (
    nomination_id   VARCHAR(10) NOT NULL,
    movie_id        NUMBER NOT NULL,
    win             NUMBER(1) NOT NULL,
    CONSTRAINT pk_nominee_movie
        PRIMARY KEY (nomination_id, movie_id),
    CONSTRAINT fk_nominee_movie_nomination
        FOREIGN KEY (nomination_id) REFERENCES Nomination(ID) 
        ON DELETE CASCADE,
    CONSTRAINT fk_nominee_movie
        FOREIGN KEY (movie_id) REFERENCES Movie(ID) 
        ON DELETE CASCADE
);

CREATE TABLE NomineePerson (
    nomination_id   VARCHAR(10) NOT NULL,
    person_id       VARCHAR(50) NOT NULL,
    win             NUMBER(1) NOT NULL,
    CONSTRAINT pk_nominee_person
        PRIMARY KEY (nomination_id, person_id),
    CONSTRAINT fk_nominee_person_nomination
        FOREIGN KEY (nomination_id) REFERENCES Nomination(ID) 
        ON DELETE CASCADE,
    CONSTRAINT fk_nominee_person
        FOREIGN KEY (person_id) REFERENCES Person(ID) 
        ON DELETE CASCADE
);

CREATE TABLE "USER" (
    ID           NUMBER NOT NULL,
    first_name   VARCHAR(50),
    last_name    VARCHAR(50),
    gender       CHAR(1),
    birthdate    DATE,
    birthplace   VARCHAR(50),
    email        VARCHAR(50),
    PRIMARY KEY (ID)
);
    
CREATE TABLE ProfilePhoto (
    name         VARCHAR(20) NOT NULL,
    author       VARCHAR(50),
    description  VARCHAR(200),
    user_id      NUMBER NOT NULL,
    CONSTRAINT pk_profile_photo
        PRIMARY KEY (name),
    CONSTRAINT fk_profile_photo_user
        FOREIGN KEY (user_id) REFERENCES "USER"(ID) 
        ON DELETE CASCADE
);
  
CREATE TABLE PersonalPhoto (
    name         VARCHAR(20) NOT NULL,
    author       VARCHAR(50),
    description  VARCHAR(200),
    person_id    VARCHAR(50) NOT NULL,
    CONSTRAINT pk_personal_photo
        PRIMARY KEY (name),
    CONSTRAINT fk_personal_photo_person
        FOREIGN KEY (person_id) REFERENCES Person(ID) 
        ON DELETE CASCADE
);

CREATE TABLE Rating (
    user_id     NUMBER NOT NULL,
    movie_id    NUMBER NOT NULL,
    stars       NUMBER(3,1) NOT NULL,
    ts          TIMESTAMP,
    CONSTRAINT pk_rating
        PRIMARY KEY (user_id, movie_id, ts),
    CONSTRAINT fk_rating_user
        FOREIGN KEY (user_id) REFERENCES "USER"(ID) 
        ON DELETE CASCADE,
    CONSTRAINT fk_rating_movie
        FOREIGN KEY (movie_id) REFERENCES Movie(ID) 
        ON DELETE CASCADE        
);

CREATE TABLE Review (
    ID              VARCHAR(10) NOT NULL,
    user_id         NUMBER NOT NULL,
    movie_id        NUMBER NOT NULL,
    pub_date        VARCHAR(50) NOT NULL,
    votes           NUMBER(10) NOT NULL,
    stars           NUMBER(2),
    CONSTRAINT pk_review
        PRIMARY KEY (ID),
    CONSTRAINT fk_review_user
        FOREIGN KEY (user_id) REFERENCES "USER"(ID) 
        ON DELETE CASCADE,
    CONSTRAINT fk_review_movie
        FOREIGN KEY (movie_id) REFERENCES Movie(ID) 
        ON DELETE CASCADE   
);

CREATE TABLE ReviewContent (
    ID              VARCHAR(10) NOT NULL,
    content_type    VARCHAR(10) NOT NULL,
    content         BLOB,
    review_id       VARCHAR(10) NOT NULL,
    CONSTRAINT pk_review_content
        PRIMARY KEY (ID),
    CONSTRAINT fk_review_content_review
        FOREIGN KEY (review_id) REFERENCES Review(ID) 
        ON DELETE CASCADE
);

CREATE TABLE "COMMENT" (
    ID              VARCHAR(10) NOT NULL,
    review_id       VARCHAR(10) NOT NULL,
    user_id         NUMBER NOT NULL,
    pub_date        TIMESTAMP NOT NULL,
    CONSTRAINT pk_comment
        PRIMARY KEY (ID),
    CONSTRAINT fk_comment_review
        FOREIGN KEY (review_id) REFERENCES Review(ID) 
        ON DELETE CASCADE,
    CONSTRAINT fk_comment_user
        FOREIGN KEY (user_id) REFERENCES "USER"(ID) 
        ON DELETE CASCADE
);

CREATE TABLE CommentContent (
    ID              VARCHAR(10) NOT NULL,
    content_type    VARCHAR(10) NOT NULL,
    content         BLOB,
    comment_id      VARCHAR(10) NOT NULL,
    CONSTRAINT pk_comment_content
        PRIMARY KEY (ID),
    CONSTRAINT fk_comment_content_comment
        FOREIGN KEY (comment_id) REFERENCES "COMMENT"(ID) 
        ON DELETE CASCADE
);

CREATE TABLE Vote (
    review_id       VARCHAR(10) NOT NULL,
    user_id         NUMBER NOT NULL,
    is_useful       NUMBER(1) NOT NULL,
    CONSTRAINT pk_vote
        PRIMARY KEY (review_id, user_id),
    CONSTRAINT fk_vote_review
        FOREIGN KEY (review_id) REFERENCES Review(ID) 
        ON DELETE CASCADE,
    CONSTRAINT fk_vote_user
        FOREIGN KEY (user_id) REFERENCES "USER"(ID) 
        ON DELETE CASCADE
);

CREATE TABLE Tag (
    ID          NUMBER NOT NULL,
    value       VARCHAR(100) NOT NULL,        
    CONSTRAINT pk_tag
        PRIMARY KEY (ID)
);

CREATE TABLE UserTaggedMovie (
    user_id     NUMBER NOT NULL,
    tag_id      NUMBER NOT NULL,
    movie_id    NUMBER NOT NULL,
    ts          TIMESTAMP,
    CONSTRAINT pk_user_tag_movie
        PRIMARY KEY (user_id, tag_id, movie_id),
    CONSTRAINT fk_user_tag_user
        FOREIGN KEY (user_id) REFERENCES "USER"(ID) 
        ON DELETE CASCADE,
    CONSTRAINT fk_user_tag_tag
        FOREIGN KEY (tag_id) REFERENCES Tag(ID) 
        ON DELETE CASCADE,
    CONSTRAINT fk_user_tag_movie
        FOREIGN KEY (movie_id) REFERENCES Movie(ID) 
        ON DELETE CASCADE
);

--
-- Create indexes
--

CREATE INDEX idx_genre ON Genres(genre);
CREATE INDEX idx_movie_year ON Movie(year);
CREATE INDEX idx_movie_country ON MovieCountry(country);
CREATE INDEX idx_person_name ON Person(full_name);
CREATE INDEX idx_rating_stars ON Rating(stars);

--
-- Create functions and others
--

CREATE SEQUENCE country_seq
    START WITH 1 INCREMENT BY 1 MINVALUE 1;

CREATE SEQUENCE location_seq
    START WITH 1 INCREMENT BY 1 MINVALUE 1;

-- Compute average rating of a movie
CREATE OR REPLACE FUNCTION movie_avg_rating (m Movie%ROWTYPE)
RETURN NUMBER IS
BEGIN
    RETURN (m.all_critics_rating + m.top_critics_rating + m.audience_rating) / 3;
END;
/

-- Compute average number of ratings of a movie
CREATE OR REPLACE FUNCTION movie_avg_num_ratings (m Movie%ROWTYPE)
RETURN NUMBER IS
BEGIN
    RETURN (m.all_critics_num_reviews + m.top_critics_num_reviews + m.audience_num_ratings) / 3;
END;
/
