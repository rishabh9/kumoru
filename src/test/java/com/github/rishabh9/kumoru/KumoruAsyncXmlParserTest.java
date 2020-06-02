package com.github.rishabh9.kumoru;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.rishabh9.kumoru.parser.ArtifactMetadata;
import com.github.rishabh9.kumoru.parser.KumoruAsyncXmlParser;
import com.github.rishabh9.kumoru.parser.SnapshotMetadata;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import java.util.ArrayList;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.api.Test;

class KumoruAsyncXmlParserTest {

  @Test
  void parseXmlExtractArtifactId_success() {
    final Buffer buffer = new BufferImpl();
    final String builder =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<metadata modelVersion=\"1.1.0\">"
            + "<artifactId>groovy</artifactId>"
            + "</metadata>";
    buffer.setBytes(0, builder.getBytes());

    final KumoruAsyncXmlParser parser = new KumoruAsyncXmlParser();
    try {
      final SnapshotMetadata metadata = parser.parse(buffer);
      assertEquals("groovy", metadata.getArtifactId());
    } catch (XMLStreamException e) {
      fail("Failed with exception", e);
    }
  }

  @Test
  void parseXml_success() {
    final Buffer buffer = new BufferImpl();
    final String builder =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<metadata modelVersion=\"1.1.0\">\n"
            + "  <groupId>org.apache.groovy</groupId>\n"
            + "  <artifactId>groovy</artifactId>\n"
            + "  <version>4.0.0-SNAPSHOT</version>\n"
            + "  <versioning>\n"
            + "    <snapshot>\n"
            + "      <timestamp>20200529.091211</timestamp>\n"
            + "      <buildNumber>217</buildNumber>\n"
            + "    </snapshot>\n"
            + "    <lastUpdated>20200529091930</lastUpdated>\n"
            + "    <snapshotVersions>\n"
            + "      <snapshotVersion>\n"
            + "        <sample>example</sample>\n"
            + "        <incorrect>placed</incorrect>\n"
            + "        <extension>jar</extension>\n"
            + "        <classifier>javadoc</classifier>\n"
            + "        <updated>20200529091211</updated>\n"
            + "        <value>4.0.0-20200529.091211-217</value>\n"
            + "      </snapshotVersion>\n"
            + "      <snapshotVersion>\n"
            + "        <classifier>sources</classifier>\n"
            + "        <value>4.0.0-20200529.091211-217</value>\n"
            + "        <hello>world</hello>\n"
            + "        <extension>jar</extension>\n"
            + "        <updated>20200529091211</updated>\n"
            + "      </snapshotVersion>\n"
            + "    </snapshotVersions>\n"
            + "  </versioning>\n"
            + "</metadata>\n";
    buffer.setBytes(0, builder.getBytes());

    final KumoruAsyncXmlParser parser = new KumoruAsyncXmlParser();
    try {
      final SnapshotMetadata metadata = parser.parse(buffer);
      assertEquals("groovy", metadata.getArtifactId());
      final Set<ArtifactMetadata> artifactsMetadata = metadata.getArtifactsMetadata();
      assertNotNull(artifactsMetadata);
      assertEquals(2, artifactsMetadata.size());
      final ArrayList<ArtifactMetadata> metaList = new ArrayList<>(artifactsMetadata);
      assertEquals("javadoc", metaList.get(0).getClassifier());
      assertEquals("jar", metaList.get(0).getExtension());
      assertEquals("20200529091211", metaList.get(0).getUpdated());
      assertEquals("4.0.0-20200529.091211-217", metaList.get(0).getValue());
      assertEquals("sources", metaList.get(1).getClassifier());
      assertEquals("jar", metaList.get(1).getExtension());
      assertEquals("20200529091211", metaList.get(1).getUpdated());
      assertEquals("4.0.0-20200529.091211-217", metaList.get(1).getValue());
    } catch (XMLStreamException e) {
      fail("Failed with exception", e);
    }
  }

  @Test
  void parseXmlExtractArtifactId_negative() {
    final Buffer buffer = new BufferImpl();
    final String builder =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<metadata modelVersion=\"1.1.0\">"
            + "</metadata>"
            + "<artifactId>groovy</artifactId>";
    buffer.setBytes(0, builder.getBytes());

    final KumoruAsyncXmlParser parser = new KumoruAsyncXmlParser();
    try {
      final SnapshotMetadata metadata = parser.parse(buffer);
      assertNull(metadata.getArtifactId());
    } catch (XMLStreamException e) {
      fail("Failed with exception", e);
    }
  }
}
