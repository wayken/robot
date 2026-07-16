import i18n from '@/locale'

const { t } = i18n.global

export const industryList = [
  {
    value: 'internet',
    label: t('industry.internet'),
    children: [
      {
        value: 'e-commerce',
        label: t('industry.e-commerce')
      },
      {
        value: 'game',
        label: t('industry.game')
      }
    ]
  },
  {
    value: 'education',
    label: t('industry.education'),
    children: [
      {
        value: 'vocational education',
        label: t('industry.vocational education')
      },
      {
        value: 'extracurricular',
        label: t('industry.extracurricular')
      }
    ]
  },
  {
    value: 'finance',
    label: t('industry.finance'),
    children: [
      {
        value: 'banking',
        label: t('industry.banking')
      },
      {
        value: 'insurance',
        label: t('industry.insurance')
      }
    ]
  },
  {
    value: 'manufacturing',
    label: t('industry.manufacturing'),
    children: [
      {
        value: 'tobacco',
        label: t('industry.tobacco')
      },
      {
        value: 'textile',
        label: t('industry.textile')
      },
      {
        value: 'railways',
        label: t('industry.railways')
      },
      {
        value: 'automobile',
        label: t('industry.automobile')
      },
      {
        value: 'petroleum',
        label: t('industry.petroleum')
      },
      {
        value: 'other manufacturing',
        label: t('industry.other manufacturing')
      }
    ]
  },
  {
    value: 'other',
    label: t('industry.other'),
    children: [
      {
        value: 'other industry',
        label: t('industry.other industry')
      }
    ]
  }
]
