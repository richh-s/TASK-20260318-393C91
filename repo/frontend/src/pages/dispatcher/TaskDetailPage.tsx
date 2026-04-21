import { useEffect, useState } from 'react';
import { Button, Card, Descriptions, Input, List, Select, Space, Tag, Typography, App as AntdApp, Form, Steps } from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import dayjs from 'dayjs';
import { apiGet, apiPost } from '../../api/client';
import type { WorkflowApprovalResponse, WorkflowTaskResponse } from '../../types';

const statusColor = (s: string) =>
  s === 'PENDING' ? 'blue'
    : s === 'APPROVED' ? 'green'
    : s === 'REJECTED' ? 'red'
    : s === 'ESCALATED' ? 'volcano'
    : 'default';

const stepFor = (s: string) =>
  s === 'PENDING' ? 0 : s === 'RETURNED' ? 0 : s === 'APPROVED' ? 2 : s === 'REJECTED' ? 2 : s === 'ESCALATED' ? 1 : 2;

export default function TaskDetailPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const { message } = AntdApp.useApp();
  const [task, setTask] = useState<WorkflowTaskResponse | null>(null);
  const [approvals, setApprovals] = useState<WorkflowApprovalResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<{ action: string; comment: string }>();

  const load = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const [t, a] = await Promise.all([
        apiGet<WorkflowTaskResponse>(`/api/workflow/tasks/${id}`),
        apiGet<WorkflowApprovalResponse[]>(`/api/workflow/tasks/${id}/approvals`),
      ]);
      setTask(t);
      setApprovals(a);
    } catch (e) {
      message.error((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [id]);

  const submit = async () => {
    try {
      const v = await form.validateFields();
      setSubmitting(true);
      await apiPost(`/api/workflow/tasks/${id}/approvals`, v);
      message.success('Action submitted');
      form.resetFields();
      load();
    } catch (e) {
      if ((e as { errorFields?: unknown }).errorFields) return;
      message.error((e as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  if (!task) {
    return <div className="page-wrapper"><Card className="page-card" loading={loading}>Loading…</Card></div>;
  }

  const canAct = task.status === 'PENDING' || task.status === 'RETURNED' || task.status === 'ESCALATED';

  return (
    <div className="page-wrapper">
      <Card className="page-card" title={<Space><Button onClick={() => nav(-1)}>← Back</Button>{task.taskNumber}</Space>}>
        <Steps
          current={stepFor(task.status)}
          status={task.status === 'REJECTED' ? 'error' : 'process'}
          items={[
            { title: 'Submitted' },
            { title: 'Under review' },
            { title: task.status === 'REJECTED' ? 'Rejected' : task.status === 'APPROVED' ? 'Approved' : 'Closed' },
          ]}
          style={{ marginBottom: 24 }}
        />
        <Descriptions bordered column={2} size="small">
          <Descriptions.Item label="Title" span={2}>{task.title}</Descriptions.Item>
          <Descriptions.Item label="Type"><Tag>{task.type.replace(/_/g, ' ')}</Tag></Descriptions.Item>
          <Descriptions.Item label="Status">
            <Space>
              <Tag color={statusColor(task.status)}>{task.status}</Tag>
              {task.escalated && <Tag color="volcano">escalated</Tag>}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="Assigned to">{task.assignedToUsername || '—'}</Descriptions.Item>
          <Descriptions.Item label="Deadline">{task.deadline ? dayjs(task.deadline).format('YYYY-MM-DD HH:mm') : '—'}</Descriptions.Item>
          <Descriptions.Item label="Created">{dayjs(task.createdAt).format('YYYY-MM-DD HH:mm:ss')}</Descriptions.Item>
          <Descriptions.Item label="Updated">{dayjs(task.updatedAt).format('YYYY-MM-DD HH:mm:ss')}</Descriptions.Item>
          {task.description && (
            <Descriptions.Item label="Description" span={2}>{task.description}</Descriptions.Item>
          )}
        </Descriptions>
      </Card>

      <Card className="page-card" title="Approval history" style={{ marginTop: 16 }}>
        <List
          dataSource={approvals}
          locale={{ emptyText: 'No actions yet' }}
          renderItem={(a) => (
            <List.Item>
              <List.Item.Meta
                title={
                  <Space>
                    <Tag color={a.action === 'APPROVE' ? 'green' : a.action === 'REJECT' ? 'red' : 'orange'}>
                      {a.action}
                    </Tag>
                    <strong>{a.approverUsername}</strong>
                    <Typography.Text type="secondary">{dayjs(a.createdAt).format('YYYY-MM-DD HH:mm')}</Typography.Text>
                  </Space>
                }
                description={a.comment || <em>No comment</em>}
              />
            </List.Item>
          )}
        />
      </Card>

      {canAct && (
        <Card className="page-card" title="Take action" style={{ marginTop: 16 }}>
          <Form form={form} layout="vertical" onFinish={submit} initialValues={{ action: 'APPROVE' }}>
            <Form.Item name="action" label="Action" rules={[{ required: true }]}>
              <Select
                options={[
                  { value: 'APPROVE', label: 'Approve' },
                  { value: 'REJECT', label: 'Reject' },
                  { value: 'RETURN', label: 'Return for resubmission' },
                ]}
              />
            </Form.Item>
            <Form.Item name="comment" label="Comment">
              <Input.TextArea rows={3} />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>Submit</Button>
          </Form>
        </Card>
      )}
    </div>
  );
}
