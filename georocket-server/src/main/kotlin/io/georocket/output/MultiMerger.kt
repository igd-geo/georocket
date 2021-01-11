package io.georocket.output

import io.georocket.storage.ChunkMeta
import io.georocket.output.xml.XMLMerger
import io.georocket.output.geojson.GeoJsonMerger
import io.georocket.storage.XMLChunkMeta
import java.lang.IllegalStateException
import io.georocket.storage.GeoJsonChunkMeta
import io.georocket.storage.ChunkReadStream
import io.vertx.core.buffer.Buffer
import io.vertx.core.streams.WriteStream
import rx.Completable

/**
 *
 * A merger that either delegates to [XMLMerger] or
 * [GeoJsonMerger] depending on the types of the chunks to merge.
 *
 * For the time being the merger can only merge chunks of the same type.
 * In the future, it may create an archive (e.g. a ZIP or a TAR file) containing
 * chunks of mixed types.
 * @param optimistic `true` if chunks should be merged optimistically
 * without prior initialization
 * @author Michel Kraemer
 */
class MultiMerger(private val optimistic: Boolean) : Merger<ChunkMeta> {
  private var xmlMerger: XMLMerger? = null
  private var geoJsonMerger: GeoJsonMerger? = null

  private fun ensureMerger(chunkMetadata: ChunkMeta): Completable {
    if (chunkMetadata is XMLChunkMeta) {
      if (xmlMerger == null) {
        if (geoJsonMerger != null) {
          return Completable.error(IllegalStateException("Cannot merge "
              + "XML chunk into a GeoJSON document."))
        }
        xmlMerger = XMLMerger(optimistic)
      }
      return Completable.complete()
    } else if (chunkMetadata is GeoJsonChunkMeta) {
      if (geoJsonMerger == null) {
        if (xmlMerger != null) {
          return Completable.error(IllegalStateException("Cannot merge "
              + "GeoJSON chunk into an XML document."))
        }
        geoJsonMerger = GeoJsonMerger(optimistic)
      }
      return Completable.complete()
    }
    return Completable.error(IllegalStateException("Cannot merge "
        + "chunk of type " + chunkMetadata.mimeType))
  }

  override fun init(chunkMetadata: ChunkMeta): Completable {
    return ensureMerger(chunkMetadata)
        .andThen(Completable.defer {
          if (chunkMetadata is XMLChunkMeta) {
            xmlMerger!!.init(chunkMetadata)
          } else {
            geoJsonMerger!!.init(chunkMetadata as GeoJsonChunkMeta)
          }
        })
  }

  override fun merge(chunk: ChunkReadStream, chunkMetadata: ChunkMeta,
      outputStream: WriteStream<Buffer>): Completable {
    return ensureMerger(chunkMetadata)
        .andThen(Completable.defer {
          if (chunkMetadata is XMLChunkMeta) {
            xmlMerger!!.merge(chunk, chunkMetadata, outputStream)
          } else {
            geoJsonMerger!!.merge(chunk, chunkMetadata as GeoJsonChunkMeta, outputStream)
          }
        })
  }

  override fun finish(outputStream: WriteStream<Buffer>) {
    if (xmlMerger != null) {
      xmlMerger!!.finish(outputStream)
    }
    if (geoJsonMerger != null) {
      geoJsonMerger!!.finish(outputStream)
    }
  }
}
