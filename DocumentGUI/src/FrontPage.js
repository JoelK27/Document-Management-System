import { 
    uploadDocumentFile, 
    getAllDocuments,
    getDocumentFileUrl,
    deleteDocument 
} from './api/client.js';

// DOM elements
const dropzone = document.getElementById('dropzone');
const fileSelectBtn = document.getElementById('fileSelect');
let fileInput = null;

// Initialize the page
document.addEventListener('DOMContentLoaded', () => {
    initFileUpload();
    initDragAndDrop();
    loadDocuments();
});

// Create hidden file input and handle button click
function initFileUpload() {
    // Create hidden file input
    fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.style.display = 'none';
    fileInput.multiple = true;
    fileInput.accept = '.pdf,.doc,.docx,.txt';
    document.body.appendChild(fileInput);
    
    // Connect button to file input
    fileSelectBtn.addEventListener('click', () => {
        fileInput.click();
    });
    
    // Handle file selection
    fileInput.addEventListener('change', (e) => {
        if (fileInput.files.length > 0) {
            handleFiles(fileInput.files);
        }
    });
}

// Setup drag and drop listeners
function initDragAndDrop() {
    // Prevent default drag behaviors
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, preventDefaults, false);
    });
    
    // Highlight drop area when item is dragged over it
    ['dragenter', 'dragover'].forEach(eventName => {
        dropzone.addEventListener(eventName, highlight, false);
    });
    
    ['dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, unhighlight, false);
    });
    
    // Handle dropped files
    dropzone.addEventListener('drop', handleDrop, false);
}

// Helper functions for drag and drop
function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
}

function highlight() {
    dropzone.classList.add('border-primary', 'bg-primary/10');
}

function unhighlight() {
    dropzone.classList.remove('border-primary', 'bg-primary/10');
}

function handleDrop(e) {
    const dt = e.dataTransfer;
    const files = dt.files;
    handleFiles(files);
}

// Upload files
async function handleFiles(files) {
    const filesArray = Array.from(files);
    
    // Create status element
    const statusElement = document.createElement('div');
    statusElement.className = 'mt-4 text-sm';
    dropzone.appendChild(statusElement);
    
    statusElement.textContent = `Uploading ${filesArray.length} file(s)...`;
    
    // Process each file
    for (const file of filesArray) {
        try {
            await uploadFile(file);
        } catch (error) {
            console.error(`Failed to upload ${file.name}:`, error);
        }
    }
    
    // Update status and refresh document list
    statusElement.textContent = `Successfully uploaded ${filesArray.length} file(s)`;
    setTimeout(() => {
        statusElement.remove();
        loadDocuments();
    }, 3000);
}

// Upload a single file
async function uploadFile(file) {
    try {
        const meta = {
            title: file.name,
            summary: `Uploaded on ${new Date().toLocaleDateString()}`,
        };
        
        const result = await uploadDocumentFile(file, meta);
        console.log('File uploaded:', result);
        return result;
    } catch (error) {
        console.error('Error uploading file:', error);
        throw error;
    }
}

// Load and display documents
async function loadDocuments() {
    try {
        const documents = await getAllDocuments();
        displayDocuments(documents);
    } catch (error) {
        console.error('Error loading documents:', error);
    }
}

// Display documents in the UI
function displayDocuments(documents) {
    const container = document.querySelector('.space-y-4');
    if (!container) return;
    
    // Clear existing content
    container.innerHTML = '';
    
    // Add each document
    documents.forEach(doc => {
        const docElement = createDocumentElement(doc);
        container.appendChild(docElement);
    });
}

// Create a document element
function createDocumentElement(doc) {
    const element = document.createElement('div');
    element.className = 'bg-black/5 dark:bg-white/5 p-4 rounded-xl flex flex-col md:flex-row items-start gap-4 transition-shadow hover:shadow-lg';
    
    // Format date for display
// Format date for display by directly manipulating the string
const uploadDate = doc.uploadDate 
    ? doc.uploadDate.replace('T', ' ').replace(/\.\d+$/, '').replace('Z', '') 
    : 'Unknown date';
    
    // Determine document status
    let statusClass = 'bg-gray-500/10 text-gray-500 dark:bg-gray-400/20 dark:text-gray-400';
    let statusDot = 'bg-gray-500';
    let statusText = 'Not Started';
    
    element.innerHTML = `
        <div class="flex-1">
            <div class="flex items-center gap-3 mb-2">
                <span class="inline-flex items-center gap-2 text-sm px-3 py-1 rounded-full ${statusClass}">
                    <span class="w-2 h-2 rounded-full ${statusDot}"></span> ${statusText}
                </span>
                <p class="text-sm text-black/60 dark:text-white/60">Uploaded on ${uploadDate}</p>
            </div>
            <h4 class="font-bold text-lg mb-2 text-black/90 dark:text-white/90">${doc.title || 'Untitled Document'}</h4>
            <p class="text-sm text-black/70 dark:text-white/70 mb-4 line-clamp-2">${doc.summary || 'No summary available'}</p>
            <div class="flex items-center gap-2">
                <button class="doc-edit p-2 rounded-lg text-black/60 dark:text-white/60 hover:bg-primary/10 dark:hover:bg-primary/20 hover:text-primary transition-colors" data-id="${doc.id}">
                    <svg fill="currentColor" height="20" viewBox="0 0 256 256" width="20" xmlns="http://www.w3.org/2000/svg">
                        <path d="M227.31 73.37 182.63 28.68a16 16 0 0 0-22.63 0L36.69 152A15.86 15.86 0 0 0 32 163.31V208a16 16 0 0 0 16 16h44.69a15.86 15.86 0 0 0 11.31-4.69L227.31 96a16 16 0 0 0 0-22.63ZM92.69 208H48v-44.69l88-88L180.69 120ZM192 108.68 147.31 64l24-24L216 84.68Z"></path>
                    </svg>
                </button>
                <button class="doc-delete p-2 rounded-lg text-black/60 dark:text-white/60 hover:bg-red-500/10 dark:hover:bg-red-500/20 hover:text-red-500 transition-colors" data-id="${doc.id}">
                    <svg fill="currentColor" height="20" viewBox="0 0 256 256" width="20" xmlns="http://www.w3.org/2000/svg">
                        <path d="M216 48h-36V40a24 24 0 0 0-24-24h-48a24 24 0 0 0-24 24v8H48a8 8 0 0 0 0 16h8v144a16 16 0 0 0 16 16h112a16 16 0 0 0 16-16V64h8a8 8 0 0 0 0-16ZM96 40a8 8 0 0 1 8-8h48a8 8 0 0 1 8 8v8H96Zm96 168H64V64h128Zm-80-104v64a8 8 0 0 1-16 0v-64a8 8 0 0 1 16 0Zm48 0v64a8 8 0 0 1-16 0v-64a8 8 0 0 1 16 0Z"></path>
                    </svg>
                </button>
                <button class="doc-download p-2 rounded-lg bg-primary/10 dark:bg-primary/20 text-primary dark:text-primary/90 hover:bg-primary/20 dark:hover:bg-primary/30 transition-colors" data-id="${doc.id}">
                    <svg width="20" height="20" viewBox="0 0 21 21" xmlns="http://www.w3.org/2000/svg">
                        <path fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" d="m6.5 10.5l4 4.232l4-4.191m-4-7.041v11m-6 3h12"/>
                    </svg>
                </button>
            </div>
        </div>
        <div class="w-full md:w-40 h-32 bg-cover bg-center rounded-lg" style="background-color: #f0f0f0;"></div>
    `;
    
    // Add event listeners for document actions
    element.querySelector('.doc-download').addEventListener('click', () => downloadDocument(doc.id));
    element.querySelector('.doc-delete').addEventListener('click', () => confirmAndDeleteDocument(doc.id));
    
    return element;
}

// Download document
async function downloadDocument(id) {
    try {
        const url = await getDocumentFileUrl(id);
        const a = document.createElement('a');
        a.href = url;
        a.download = `document-${id}`;
        a.target = '_blank';
        a.click();
    } catch (error) {
        console.error('Error downloading document:', error);
    }
}

// Create modal element for delete confirmation
function createDeleteModal() {
    const modal = document.createElement('div');
    modal.id = 'delete-confirmation-modal';
    modal.className = 'fixed inset-0 bg-black/30 dark:bg-black/50 flex items-center justify-center p-4 z-50';
    modal.style.display = 'none';
    
    modal.innerHTML = `
        <div class="bg-background-light dark:bg-background-dark rounded-xl p-8 max-w-sm w-full text-center shadow-2xl">
            <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-red-500/10 mb-4">
                <svg class="h-8 w-8 text-red-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" stroke-linecap="round" stroke-linejoin="round"></path>
                </svg>
            </div>
            <h3 class="text-xl font-bold text-black/90 dark:text-white/90 mb-2">Are you sure?</h3>
            <p class="text-black/60 dark:text-white/60 mb-6">This action will permanently delete the document. This cannot be undone.</p>
            <div class="flex gap-4">
                <button id="delete-cancel-btn" class="flex-1 px-6 py-3 bg-black/5 dark:bg-white/10 text-black/80 dark:text-white/80 font-bold rounded-lg hover:bg-black/10 dark:hover:bg-white/20 transition-colors">Cancel</button>
                <button id="delete-confirm-btn" class="flex-1 px-6 py-3 bg-red-500/80 text-white font-bold rounded-lg hover:bg-red-500 transition-colors">Delete</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    return modal;
}

// Initialize delete modal on page load
let deleteModal = null;
let pendingDeleteId = null;

document.addEventListener('DOMContentLoaded', () => {
    // Add this to your existing DOMContentLoaded event
    deleteModal = createDeleteModal();
    
    // Add event listeners for the modal buttons
    document.getElementById('delete-cancel-btn').addEventListener('click', () => {
        hideDeleteModal();
    });
    
    document.getElementById('delete-confirm-btn').addEventListener('click', () => {
        if (pendingDeleteId !== null) {
            proceedWithDelete(pendingDeleteId);
        }
    });
});

// Show the delete confirmation modal
function showDeleteModal(id) {
    pendingDeleteId = id;
    deleteModal.style.display = 'flex';
}

// Hide the delete confirmation modal
function hideDeleteModal() {
    pendingDeleteId = null;
    deleteModal.style.display = 'none';
}

// Actually delete the document after confirmation
function proceedWithDelete(id) {
    deleteDocument(id)
        .then(() => {
            loadDocuments();
            hideDeleteModal();
        })
        .catch(error => {
            console.error('Error deleting document:', error);
            hideDeleteModal();
        });
}

// Replace the existing confirmAndDeleteDocument function with this one
function confirmAndDeleteDocument(id) {
    showDeleteModal(id);
}

export { initFileUpload, initDragAndDrop, loadDocuments };