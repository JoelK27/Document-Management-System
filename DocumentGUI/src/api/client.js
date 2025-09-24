import axios from 'axios';

export const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

/**
 * @typedef {Object} DocumentDto
 * @property {number=} id
 * @property {string=} title
 * @property {string=} summary
 * @property {string=} content
 * @property {string=} uploadDate
 * @property {string=} fileName
 * @property {string=} mimeType
 * @property {number=} size
 */

/** Get by id (ohne fileData) */
export const getDocument = async (documentId) => {
  const { data } = await api.get(`/documents/${documentId}`);
  return data;
};

/** List or search documents (q optional) */
export const searchDocuments = async (q = '') => {
  const url = q ? `/documents/search?q=${encodeURIComponent(q)}` : `/documents/search`;
  const { data } = await api.get(url);
  return data; // Array<DocumentDto>
};

/** Alle Dokumente (dedizierter Endpoint) */
export const getAllDocuments = async () => {
  const { data } = await api.get('/documents');
  return data; // Array<DocumentDto>
};

export const listDocuments = () => searchDocuments('');

/** Create (JSON) */
export const createDocument = async (documentData /** @type {DocumentDto} */) => {
  const { data } = await api.post('/documents/upload', documentData);
  return data;
};

/** Update (JSON) */
export const updateDocument = async (documentId, documentData /** @type {DocumentDto} */) => {
  const { data } = await api.put(`/documents/${documentId}`, documentData);
  return data;
};

/** Delete */
export const deleteDocument = async (documentId) => {
  await api.delete(`/documents/${documentId}`);
  return true;
};

/** Upload Datei (multipart) + optionale Metadaten */
export const uploadDocumentFile = async (file, meta = {}) => {
  const fd = new FormData();
  fd.append('file', file);
  if (meta.title) fd.append('title', meta.title);
  if (meta.summary) fd.append('summary', meta.summary);
  if (meta.content) fd.append('content', meta.content);
  const { data } = await api.post('/documents/upload-file', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data; // DocumentDto
};

/** Datei ersetzen (multipart) */
export const replaceDocumentFile = async (documentId, file) => {
  const fd = new FormData();
  fd.append('file', file);
  const { data } = await api.put(`/documents/${documentId}/file`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
};

/** Datei herunterladen als Blob */
export const downloadDocumentFile = async (documentId) => {
  const res = await api.get(`/documents/${documentId}/file`, { responseType: 'blob' });
  return res.data; // Blob
};

/** Hilfsfunktion: URL zum Anzeigen/Herunterladen im Browser */
export const getDocumentFileUrl = async (documentId) => {
  const blob = await downloadDocumentFile(documentId);
  return URL.createObjectURL(blob);
};

/** Vorschau herunterladen als Blob */
export const getDocumentPreviewBlob = async (documentId) => {
  const res = await api.get(`/documents/${documentId}/preview`, { responseType: 'blob' });
  return res.data;
};