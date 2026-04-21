import { useEffect, useState } from 'react';
import { Button, Card, Select, Space, Table, Tag, Upload, App as AntdApp } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import { UploadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { apiGet, apiUpload } from '../../api/client';
import type { Page } from '../../types';

interface ImportRecord {
  id: number;
  importType: string;
  status: string;
  fileName: string;
  rowsParsed: number;
  rowsFailed: number;
  createdAt: string;
}

const statusColor = (s: string) =>
  s === 'DONE' || s === 'PARSED' ? 'green'
    : s === 'FAILED' ? 'red'
    : s === 'PARSING' || s === 'PROCESSING' ? 'blue'
    : 'default';

export default function ImportsPage() {
  const { message } = AntdApp.useApp();
  const [data, setData] = useState<ImportRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [importType, setImportType] = useState<'JSON' | 'HTML'>('JSON');
  const [uploading, setUploading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const res = await apiGet<Page<ImportRecord>>('/api/bus-data/imports', { page, size });
      setData(res.content);
      setTotal(res.totalElements);
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [page, size]);

  const upload = async () => {
    const file = fileList[0]?.originFileObj;
    if (!file) {
      message.warning('Pick a file first');
      return;
    }
    const fd = new FormData();
    fd.append('file', file);
    fd.append('importType', importType);
    setUploading(true);
    try {
      await apiUpload('/api/bus-data/imports', fd);
      message.success('Upload queued — processing async');
      setFileList([]);
      load();
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setUploading(false);
    }
  };

  const columns: ColumnsType<ImportRecord> = [
    { title: 'File', dataIndex: 'fileName' },
    { title: 'Type', dataIndex: 'importType', render: (v) => <Tag>{v}</Tag> },
    { title: 'Status', dataIndex: 'status', render: (v) => <Tag color={statusColor(v)}>{v}</Tag> },
    { title: 'Parsed', dataIndex: 'rowsParsed' },
    { title: 'Failed', dataIndex: 'rowsFailed', render: (v) => <Tag color={v > 0 ? 'red' : 'default'}>{v}</Tag> },
    { title: 'Created', dataIndex: 'createdAt', render: (v) => dayjs(v).format('YYYY-MM-DD HH:mm') },
  ];

  return (
    <div className="page-wrapper">
      <Card className="page-card" title="Upload bus data">
        <Space>
          <Select
            value={importType}
            onChange={setImportType}
            options={[{ value: 'JSON', label: 'JSON' }, { value: 'HTML', label: 'HTML' }]}
            style={{ width: 120 }}
          />
          <Upload
            beforeUpload={() => false}
            maxCount={1}
            fileList={fileList}
            onChange={({ fileList }) => setFileList(fileList)}
          >
            <Button icon={<UploadOutlined />}>Select file</Button>
          </Upload>
          <Button type="primary" onClick={upload} loading={uploading} disabled={fileList.length === 0}>
            Upload
          </Button>
          <Button onClick={load}>Refresh</Button>
        </Space>
      </Card>

      <Card className="page-card" title="Import history" style={{ marginTop: 16 }}>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={data}
          pagination={{
            current: page + 1,
            pageSize: size,
            total,
            onChange: (p, s) => { setPage(p - 1); setSize(s); },
          }}
        />
      </Card>
    </div>
  );
}
