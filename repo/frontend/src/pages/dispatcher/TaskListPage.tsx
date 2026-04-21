import { useEffect, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, DatePicker, App as AntdApp } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { TableRowSelection } from 'antd/es/table/interface';
import dayjs, { Dayjs } from 'dayjs';
import { Link } from 'react-router-dom';
import { apiGet, apiPost } from '../../api/client';
import type { Page, WorkflowTaskResponse } from '../../types';

const TASK_TYPES = [
  { value: 'ROUTE_DATA_CHANGE', label: 'Route Data Change' },
  { value: 'REMINDER_RULE_CONFIG', label: 'Reminder Rule Config' },
  { value: 'ABNORMAL_DATA_REVIEW', label: 'Abnormal Data Review' },
];

const STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'RETURNED', 'ESCALATED', 'CANCELLED'];

const statusColor = (s: string) =>
  s === 'PENDING' ? 'blue'
    : s === 'APPROVED' ? 'green'
    : s === 'REJECTED' ? 'red'
    : s === 'ESCALATED' ? 'volcano'
    : 'default';

export default function TaskListPage() {
  const { message } = AntdApp.useApp();
  const [data, setData] = useState<WorkflowTaskResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [filterStatus, setFilterStatus] = useState<string | undefined>();
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [batchModalOpen, setBatchModalOpen] = useState(false);
  const [batchProcessing, setBatchProcessing] = useState(false);
  const [batchForm] = Form.useForm<{ action: string; comment?: string }>();
  const [form] = Form.useForm<{
    type: string; title: string; description?: string;
    assignedToId?: number; deadline?: Dayjs;
  }>();

  const load = async () => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { page, size };
      if (filterStatus) params.status = filterStatus;
      const res = await apiGet<Page<WorkflowTaskResponse>>('/api/workflow/tasks', params);
      setData(res.content);
      setTotal(res.totalElements);
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [page, size, filterStatus]);

  const submit = async () => {
    try {
      const v = await form.validateFields();
      await apiPost('/api/workflow/tasks', {
        type: v.type,
        title: v.title,
        description: v.description,
        assignedToId: v.assignedToId || null,
        deadline: v.deadline ? v.deadline.format('YYYY-MM-DDTHH:mm:ss') : null,
      });
      message.success('Task created');
      setModalOpen(false);
      form.resetFields();
      load();
    } catch (e) {
      if ((e as { errorFields?: unknown }).errorFields) return;
      message.error((e as Error).message);
    }
  };

  const submitBatch = async () => {
    try {
      const v = await batchForm.validateFields();
      setBatchProcessing(true);
      let ok = 0;
      let fail = 0;
      for (const id of selectedIds) {
        try {
          await apiPost(`/api/workflow/tasks/${id}/approvals`, { action: v.action, comment: v.comment || '' });
          ok++;
        } catch {
          fail++;
        }
      }
      message.success(`Processed ${ok} task(s)${fail > 0 ? `, ${fail} failed` : ''}`);
      setBatchModalOpen(false);
      batchForm.resetFields();
      setSelectedIds([]);
      load();
    } catch (e) {
      if ((e as { errorFields?: unknown }).errorFields) return;
      message.error((e as Error).message);
    } finally {
      setBatchProcessing(false);
    }
  };

  const rowSelection: TableRowSelection<WorkflowTaskResponse> = {
    selectedRowKeys: selectedIds,
    onChange: (keys) => setSelectedIds(keys as number[]),
    getCheckboxProps: (r) => ({ disabled: r.status !== 'PENDING' }),
  };

  const columns: ColumnsType<WorkflowTaskResponse> = [
    { title: 'Task #', dataIndex: 'taskNumber' },
    { title: 'Title', render: (_, t) => <Link to={`/dispatcher/tasks/${t.id}`}>{t.title}</Link> },
    { title: 'Type', dataIndex: 'type', render: (v) => <Tag>{v.replace(/_/g, ' ')}</Tag> },
    {
      title: 'Status',
      dataIndex: 'status',
      render: (s, r) => (
        <Space>
          <Tag color={statusColor(s)}>{s}</Tag>
          {r.escalated && <Tag color="volcano">escalated</Tag>}
        </Space>
      ),
    },
    { title: 'Assigned to', dataIndex: 'assignedToUsername', render: (v) => v || '—' },
    { title: 'Deadline', dataIndex: 'deadline', render: (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '—') },
  ];

  return (
    <div className="page-wrapper">
      <Card
        className="page-card"
        title="All tasks"
        extra={
          <Space>
            <Select
              allowClear
              placeholder="Filter by status"
              style={{ width: 180 }}
              options={STATUSES.map((s) => ({ value: s, label: s }))}
              onChange={(v) => { setFilterStatus(v); setPage(0); }}
            />
            {selectedIds.length > 0 && (
              <Button onClick={() => setBatchModalOpen(true)}>
                Batch action ({selectedIds.length})
              </Button>
            )}
            <Button type="primary" onClick={() => setModalOpen(true)}>
              New task
            </Button>
          </Space>
        }
      >
        <Table
          rowKey="id"
          rowSelection={rowSelection}
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

      <Modal
        title={`Batch action — ${selectedIds.length} task(s)`}
        open={batchModalOpen}
        onOk={submitBatch}
        okButtonProps={{ loading: batchProcessing }}
        onCancel={() => { setBatchModalOpen(false); batchForm.resetFields(); }}
        okText="Apply to all"
        destroyOnClose
      >
        <Form form={batchForm} layout="vertical">
          <Form.Item name="action" label="Action" rules={[{ required: true, message: 'Required' }]}>
            <Select options={[
              { value: 'APPROVE', label: 'Approve' },
              { value: 'REJECT', label: 'Reject' },
              { value: 'RETURN', label: 'Return for resubmission' },
            ]} />
          </Form.Item>
          <Form.Item name="comment" label="Comment">
            <Input.TextArea rows={3} placeholder="Optional" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Create workflow task"
        open={modalOpen}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        okText="Create"
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="type" label="Type" rules={[{ required: true, message: 'Required' }]}>
            <Select options={TASK_TYPES} />
          </Form.Item>
          <Form.Item name="title" label="Title" rules={[{ required: true, message: 'Required' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="assignedToId" label="Assigned to (user ID)">
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="deadline" label="Deadline">
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
