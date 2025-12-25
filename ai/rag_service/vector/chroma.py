import chromadb
from chromadb.config import Settings
from chromadb.utils.embedding_functions import SentenceTransformerEmbeddingFunction
from typing import List, Dict, Any
import logging
import os
import warnings

# Chroma telemetry 및 tokenizer 병렬 비활성화
os.environ["CHROMA_TELEMETRY_DISABLED"] = "true"
os.environ["CHROMADB_TELEMETRY_DISABLED"] = "true"
os.environ["TOKENIZERS_PARALLELISM"] = "false"
warnings.filterwarnings('ignore')

# Telemetry 로거 비활성화
logging.getLogger('chromadb.telemetry').setLevel(logging.CRITICAL)
logging.getLogger('chromadb.telemetry.posthog').setLevel(logging.CRITICAL)

try:
    from rag_service.config import config
except ImportError:
    from config import config

logger = logging.getLogger(__name__)


class ChromaClient:
    """Chroma 벡터 데이터베이스 클라이언트"""

    def __init__(self):
        """Chroma Client 초기화 (0.3.21 호환)"""
        os.makedirs(config.CHROMA_DIR, exist_ok=True)

        try:
            # Persistent duckdb+parquet 저장소 사용
            self.client = chromadb.Client(
                Settings(
                    chroma_db_impl="duckdb+parquet",
                    persist_directory=config.CHROMA_DIR,
                )
            )

            # 임베딩 함수 지정 (SentenceTransformer)
            self.embedding_fn = SentenceTransformerEmbeddingFunction(
                model_name="all-MiniLM-L6-v2"
            )

            # 컬렉션 생성 또는 가져오기 (없으면 자동 생성)
            self.collection = self.client.get_or_create_collection(
                name=config.CHROMA_COLLECTION,
                embedding_function=self.embedding_fn,
            )

            logger.info(
                f"✅ Chroma 클라이언트 초기화 완료 (컬렉션: {config.CHROMA_COLLECTION})")
        except Exception as e:
            logger.error(f"❌ Chroma 초기화 실패: {e}")
            raise

    async def search(
        self,
        query_text: str,
        top_k: int = 50,
    ) -> List[str]:
        """
        텍스트 쿼리로 유사 음식 검색

        Args:
            query_text: 검색 쿼리 (예: "고단백 저나트륨 저칼로리")
            top_k: 반환할 상위 개수

        Returns:
            Food ID 리스트 (유사도 순서)
        """
        try:
            # 컬렉션이 없으면 재생성
            if self.collection is None:
                self.collection = self.client.get_or_create_collection(
                    name=config.CHROMA_COLLECTION,
                    embedding_function=self.embedding_fn,
                )

            results = self.collection.query(
                query_texts=[query_text],
                n_results=top_k
            )

            # results 구조: {'ids': [[...]], 'distances': [[...]]}
            food_ids = results['ids'][0] if results['ids'] else []

            logger.info(f"✅ Chroma 검색 완료: '{query_text}' → {len(food_ids)}개")
            return food_ids
        except Exception as e:
            logger.error(f"❌ Chroma 검색 실패: {e}")
            return []

    async def upsert(
        self,
        ids: List[str],
        documents: List[str],
        metadatas: List[Dict[str, Any]]
    ) -> None:
        """
        문서와 메타데이터 저장 (또는 업데이트)
        chromadb 0.3.21에서는 add() 메서드 사용

        Args:
            ids: 문서 ID 리스트
            documents: 문서 텍스트 리스트
            metadatas: 메타데이터 리스트
        """
        try:
            if self.collection is None:
                self.collection = self.client.get_or_create_collection(
                    name=config.CHROMA_COLLECTION,
                    embedding_function=self.embedding_fn,
                )

            # 기존 ID 제거 (있으면)
            try:
                self.collection.delete(ids=ids)
            except Exception:
                pass  # ID가 없으면 무시

            # 새로운 문서 추가
            self.collection.add(
                ids=ids,
                documents=documents,
                metadatas=metadatas
            )
            # 디스크에 저장
            try:
                self.client.persist()
            except Exception:
                pass
            logger.info(f"✅ Chroma add 완료: {len(ids)}개 문서")
        except Exception as e:
            logger.error(f"❌ Chroma add 실패: {e}")

    async def delete_collection(self) -> None:
        """컬렉션 삭제"""
        try:
            self.client.delete_collection(name=self.collection_name)
            logger.info(f"✅ 컬렉션 삭제: {self.collection_name}")
        except Exception as e:
            logger.error(f"❌ 컬렉션 삭제 실패: {e}")


# 싱글톤 인스턴스
_chroma_client = None


def get_chroma_client() -> ChromaClient:
    """ChromaClient 싱글톤 반환"""
    global _chroma_client
    if _chroma_client is None:
        _chroma_client = ChromaClient()
    return _chroma_client
