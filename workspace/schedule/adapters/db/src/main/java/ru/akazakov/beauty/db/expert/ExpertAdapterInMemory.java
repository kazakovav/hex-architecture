package ru.akazakov.beauty.db.expert;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializerObjectArray;
import org.springframework.stereotype.Repository;
import ru.akazakov.beauty.application.expert.port.GetAllExpertsPort;
import ru.akazakov.beauty.application.expert.port.SaveExpertPort;
import ru.akazakov.beauty.domain.expert.Expert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
public class ExpertAdapterInMemory implements SaveExpertPort, GetAllExpertsPort {
    private static final String DB_FILE_NAME = "experts.db";
    private static final String MAP_NAME = "experts";

    private final ExpertSerialzer expertSerialzer;

    public ExpertAdapterInMemory(ObjectMapper objectMapper) {
        this.expertSerialzer = new ExpertSerialzer(objectMapper);
    }

    @Override
    public void saveExpert(Expert expert) {
        log.info("Saving expert: {}", expert);
        try (DB db = DBMaker.fileDB(DB_FILE_NAME).make()) {
            HTreeMap<UUID, Expert> experts = db.hashMap(MAP_NAME).keySerializer(Serializer.UUID)
                    .valueSerializer(expertSerialzer).createOrOpen();

            experts.put(expert.getId().getValue(), expert);
        }
    }

    @Override
    public List<Expert> getAll() {
        log.info("Get all experts");
        try (DB db = DBMaker.fileDB(DB_FILE_NAME).make()) {
            HTreeMap<UUID, Expert> experts = db.hashMap(MAP_NAME).keySerializer(Serializer.UUID)
                    .valueSerializer(expertSerialzer).createOrOpen();

            return new ArrayList<>(experts.values());
        }
    }

    @RequiredArgsConstructor
    static class ExpertSerialzer extends GroupSerializerObjectArray<Expert> {
        private final ObjectMapper objectMapper;

        @Override
        public void serialize(DataOutput2 dataOutput2, Expert expert) throws IOException {
            dataOutput2.writeUTF(objectMapper.writeValueAsString(expert));
        }

        @Override
        public Expert deserialize(DataInput2 dataInput2, int i) throws IOException {
            return objectMapper.readValue(dataInput2.readUTF(), Expert.class);
        }
    }
}
