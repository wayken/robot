import {
  Session,
  Document,
  Knowledge,
  FileType,
  Trash
} from '@/types'
import { Dexie, type EntityTable } from 'dexie'

export const db = new Dexie('WikiStudio') as Dexie & {
  sessions: EntityTable<Pick<Session, 'id' | 'messages'>, 'id'>,
  documents: EntityTable<Document, 'id'>
  knowledge: EntityTable<Knowledge, 'id'>
  files: EntityTable<FileType, 'id'>
  trash: EntityTable<Trash, 'id'>
}

db.version(1).stores({
  sessions: '&id, messages',
  documents: '&id, *path, option',
  knowledge: '&id, name, model, files, updated, description',
  files: 'id, name, path, size, ext, type, modified',
  trash: '&id, source, destination, type, updated'
})
